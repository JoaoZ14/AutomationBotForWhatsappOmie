package com.whatsappomie.bot.service;

import com.whatsappomie.bot.domain.Atendimento;
import com.whatsappomie.bot.domain.Cliente;
import com.whatsappomie.bot.domain.StatusAtendimento;
import com.whatsappomie.bot.integration.whatsapp.WhatsAppService;
import com.whatsappomie.bot.repository.ClienteRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private static final int MAX_TENTATIVAS_REENVIO = 3;

    private final ClienteRepository clienteRepository;
    private final AtendimentoService atendimentoService;
    private final WhatsAppService whatsAppService;

    public SchedulerService(
            ClienteRepository clienteRepository,
            AtendimentoService atendimentoService,
            WhatsAppService whatsAppService) {
        this.clienteRepository = clienteRepository;
        this.atendimentoService = atendimentoService;
        this.whatsAppService = whatsAppService;
    }

    /**
     * Campanha semanal (ex.: segunda 09:00). Cron desligado até configurar o provedor WhatsApp.
     *
     * <pre>
     * // @Scheduled(cron = "0 0 9 * * MON", zone = "America/Sao_Paulo")
     * </pre>
     */
    @Transactional
    public void enviarCampanhaSegunda() {
        log.info("Iniciando campanha semanal (mock)");
        List<Cliente> ativos = clienteRepository.findByAtivoTrue();
        for (Cliente c : ativos) {
            whatsAppService.enviarMensagem(
                    c.getTelefone(), "Olá! Que tal revisar seu pedido desta semana? Responda esta mensagem para começar.");
        }
    }

    @Scheduled(fixedDelayString = "${app.scheduler.reenvio-ms:3600000}")
    @Transactional
    public void reenviarNaoRespondidos() {
        Instant limite = Instant.now().minus(Duration.ofHours(24));
        List<StatusAtendimento> alvo = List.of(
                StatusAtendimento.AGUARDANDO_CONFIRMACAO,
                StatusAtendimento.AGUARDANDO_ESCOLHA_TIPO,
                StatusAtendimento.AGUARDANDO_PRODUTO,
                StatusAtendimento.AGUARDANDO_QUANTIDADE,
                StatusAtendimento.AGUARDANDO_CONFIRMACAO_PEDIDO);
        List<Atendimento> pendentes =
                atendimentoService.listarParaReenvio(alvo, limite, MAX_TENTATIVAS_REENVIO);
        for (Atendimento a : pendentes) {
            whatsAppService.enviarMensagem(
                    a.getTelefone(), "Ainda estamos aguardando sua resposta para seguir com o pedido.");
            atendimentoService.incrementarTentativa(a.getId());
        }
    }

    @Scheduled(fixedDelayString = "${app.scheduler.expiracao-ms:600000}")
    @Transactional
    public void expirarAtendimentos() {
        Instant agora = Instant.now();
        List<Atendimento> vencidos = atendimentoService.listarExpiradosPendentes(agora);
        for (Atendimento a : vencidos) {
            atendimentoService.expirarAtendimento(a.getId());
            whatsAppService.enviarMensagem(
                    a.getTelefone(), "Seu atendimento foi encerrado por falta de resposta. Envie uma nova mensagem quando quiser.");
        }
    }
}
