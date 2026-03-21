package com.whatsappomie.bot.service;

import com.whatsappomie.bot.domain.Atendimento;
import com.whatsappomie.bot.domain.StatusAtendimento;
import com.whatsappomie.bot.repository.AtendimentoRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtendimentoService {

    private static final Duration PADRAO_EXPIRACAO = Duration.ofHours(48);

    private final AtendimentoRepository atendimentoRepository;

    public AtendimentoService(AtendimentoRepository atendimentoRepository) {
        this.atendimentoRepository = atendimentoRepository;
    }

    @Transactional
    public Atendimento buscarOuCriarPorTelefone(String telefone) {
        return atendimentoRepository
                .findFirstByTelefoneOrderByIdDesc(telefone)
                .filter(a -> a.getStatus() != StatusAtendimento.FINALIZADO
                        && a.getStatus() != StatusAtendimento.EXPIRADO)
                .orElseGet(() -> criarNovoAtendimento(telefone));
    }

    /**
     * Último atendimento ainda aberto ou novo em {@link StatusAtendimento#INICIADO} — usado para o bot falar primeiro.
     */
    @Transactional
    public Atendimento obterOuCriarParaDisparoProativo(String telefone) {
        return atendimentoRepository
                .findFirstByTelefoneOrderByIdDesc(telefone)
                .filter(a -> a.getStatus() != StatusAtendimento.FINALIZADO
                        && a.getStatus() != StatusAtendimento.EXPIRADO)
                .orElseGet(() -> criarNovoAtendimento(telefone));
    }

    private Atendimento criarNovoAtendimento(String telefone) {
        Instant agora = Instant.now();
        Atendimento novo = new Atendimento();
        novo.setTelefone(telefone);
        novo.setStatus(StatusAtendimento.INICIADO);
        novo.setTentativas(0);
        novo.setUltimoContato(agora);
        novo.setExpiraEm(agora.plus(PADRAO_EXPIRACAO));
        novo.setContextoJson("{}");
        return atendimentoRepository.save(novo);
    }

    @Transactional
    public Atendimento atualizarStatus(Long atendimentoId, StatusAtendimento novoStatus) {
        Atendimento a = atendimentoRepository
                .findById(atendimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Atendimento não encontrado: " + atendimentoId));
        a.setStatus(novoStatus);
        a.setUltimoContato(Instant.now());
        return atendimentoRepository.save(a);
    }

    @Transactional
    public Atendimento atualizarStatus(Atendimento atendimento, StatusAtendimento novoStatus) {
        return atualizarStatus(atendimento.getId(), novoStatus);
    }

    @Transactional
    public void incrementarTentativa(Long atendimentoId) {
        Atendimento a = atendimentoRepository
                .findById(atendimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Atendimento não encontrado: " + atendimentoId));
        int t = a.getTentativas() != null ? a.getTentativas() : 0;
        a.setTentativas(t + 1);
        a.setUltimoContato(Instant.now());
        atendimentoRepository.save(a);
    }

    @Transactional
    public void expirarAtendimento(Long atendimentoId) {
        atualizarStatus(atendimentoId, StatusAtendimento.EXPIRADO);
    }

    @Transactional(readOnly = true)
    public List<Atendimento> listarExpiradosPendentes(Instant referencia) {
        return atendimentoRepository.findByExpiraEmBeforeAndStatusNotIn(
                referencia, List.of(StatusAtendimento.FINALIZADO, StatusAtendimento.EXPIRADO));
    }

    @Transactional(readOnly = true)
    public List<Atendimento> listarParaReenvio(
            List<StatusAtendimento> status,
            Instant ultimoContatoAntesDe,
            int maxTentativas) {
        return atendimentoRepository.findByStatusInAndUltimoContatoBeforeAndTentativasLessThan(
                status, ultimoContatoAntesDe, maxTentativas);
    }

    @Transactional
    public Atendimento salvar(Atendimento atendimento) {
        return atendimentoRepository.save(atendimento);
    }

    @Transactional
    public Atendimento tocarUltimoContato(Atendimento atendimento) {
        atendimento.setUltimoContato(Instant.now());
        return atendimentoRepository.save(atendimento);
    }
}
