package com.whatsappomie.bot.service;

import com.whatsappomie.bot.domain.Atendimento;
import com.whatsappomie.bot.domain.Cliente;
import com.whatsappomie.bot.domain.Pedido;
import com.whatsappomie.bot.domain.StatusAtendimento;
import com.whatsappomie.bot.dto.ProdutoCatalogoDto;
import com.whatsappomie.bot.dto.omie.OmiePedidoCompraRequest;
import com.whatsappomie.bot.dto.omie.OmiePedidoItemRequest;
import com.whatsappomie.bot.integration.omie.OmieService;
import com.whatsappomie.bot.integration.whatsapp.WhatsAppService;
import com.whatsappomie.bot.util.TelefoneNormalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

    private static final Pattern PEDIDO_ID_PATTERN = Pattern.compile("\"pedidoId\"\\s*:\\s*(\\d+)");
    private static final Pattern CODIGO_PRODUTO_PATTERN =
            Pattern.compile("\"codigoProduto\"\\s*:\\s*\"([^\"]+)\"");

    private static final String MSG_BOAS_VINDAS_PEDIDO =
            "Bom dia! Deseja realizar seu pedido?\n\n1 - Sim\n2 - Não";

    private static final String MSG_ESCOLHA_TIPO =
            "Você deseja:\n\n1 - Repetir último pedido\n2 - Fazer novo pedido";

    private static final String MSG_OPCAO_INVALIDA_CONFIRMACAO =
            "Não entendi. Responda apenas 1 para Sim ou 2 para Não.";

    private static final String MSG_OPCAO_INVALIDA_TIPO =
            "Não entendi. Responda 1 para repetir o último pedido ou 2 para fazer um novo pedido.";

    private static final String MSG_SEM_PEDIDO_PARA_REPETIR =
            "Não encontramos um pedido anterior para repetir. Responda 2 para fazer um novo pedido.";

    private final AtendimentoService atendimentoService;
    private final ClienteService clienteService;
    private final PedidoService pedidoService;
    private final ProdutoService produtoService;
    private final WhatsAppService whatsAppService;
    private final OmieService omieService;

    public ConversationService(
            AtendimentoService atendimentoService,
            ClienteService clienteService,
            PedidoService pedidoService,
            ProdutoService produtoService,
            WhatsAppService whatsAppService,
            OmieService omieService) {
        this.atendimentoService = atendimentoService;
        this.clienteService = clienteService;
        this.pedidoService = pedidoService;
        this.produtoService = produtoService;
        this.whatsAppService = whatsAppService;
        this.omieService = omieService;
    }

    /**
     * Envia a mensagem inicial (menu 1/2) e posiciona o atendimento em {@link StatusAtendimento#AGUARDANDO_CONFIRMACAO}.
     * Chame este endpoint antes de esperar a primeira resposta do cliente no WhatsApp.
     */
    @Transactional
    public void dispararMensagemInicial(String telefoneRaw) {
        String tel = TelefoneNormalizer.paraE164(telefoneRaw.trim());
        if (tel.isEmpty()) {
            throw new IllegalArgumentException("Telefone inválido.");
        }
        Atendimento atendimento = atendimentoService.obterOuCriarParaDisparoProativo(tel);
        if (atendimento.getStatus() != StatusAtendimento.INICIADO
                && atendimento.getStatus() != StatusAtendimento.AGUARDANDO_CONFIRMACAO) {
            throw new IllegalStateException(
                    "Já existe um atendimento em andamento neste número. Conclua o fluxo ou aguarde expirar antes de um novo disparo.");
        }
        if (atendimento.getStatus() == StatusAtendimento.INICIADO) {
            enviarBoasVindasEPassoConfirmacao(atendimento);
        } else {
            whatsAppService.enviarMensagem(atendimento.getTelefone(), MSG_BOAS_VINDAS_PEDIDO);
            atendimentoService.tocarUltimoContato(atendimento);
        }
    }

    @Transactional
    public void processarMensagem(String telefone, String mensagem) {
        String tel = TelefoneNormalizer.paraE164(telefone.trim());
        String msg = mensagem != null ? mensagem.trim() : "";

        Atendimento atendimento = atendimentoService.buscarOuCriarPorTelefone(tel);
        atendimentoService.tocarUltimoContato(atendimento);

        switch (atendimento.getStatus()) {
            case INICIADO -> processarIniciado(atendimento, msg);
            case AGUARDANDO_CONFIRMACAO -> fluxoConfirmacao(atendimento, msg);
            case AGUARDANDO_ESCOLHA_TIPO -> fluxoEscolhaTipo(atendimento, msg);
            case AGUARDANDO_PRODUTO -> processarAguardandoProduto(atendimento, msg);
            case AGUARDANDO_QUANTIDADE -> processarAguardandoQuantidade(atendimento, msg);
            case AGUARDANDO_CONFIRMACAO_PEDIDO -> processarAguardandoConfirmacaoPedido(atendimento, msg);
            case FINALIZADO -> processarFinalizado(atendimento, msg);
            case EXPIRADO -> processarExpirado(atendimento, msg);
        }
    }

    private void processarIniciado(Atendimento atendimento, String ignored) {
        enviarBoasVindasEPassoConfirmacao(atendimento);
    }

    private void enviarBoasVindasEPassoConfirmacao(Atendimento atendimento) {
        atendimentoService.atualizarStatus(atendimento, StatusAtendimento.AGUARDANDO_CONFIRMACAO);
        whatsAppService.enviarMensagem(atendimento.getTelefone(), MSG_BOAS_VINDAS_PEDIDO);
    }

    private void fluxoConfirmacao(Atendimento atendimento, String mensagem) {
        parseOpcaoUmOuDois(mensagem)
                .ifPresentOrElse(
                        opcao -> {
                            if (opcao == 1) {
                                clienteService.obterOuCriarPorTelefone(atendimento.getTelefone());
                                atendimentoService.atualizarStatus(
                                        atendimento, StatusAtendimento.AGUARDANDO_ESCOLHA_TIPO);
                                whatsAppService.enviarMensagem(atendimento.getTelefone(), MSG_ESCOLHA_TIPO);
                            } else {
                                atendimentoService.atualizarStatus(atendimento, StatusAtendimento.FINALIZADO);
                                whatsAppService.enviarMensagem(
                                        atendimento.getTelefone(),
                                        "Atendimento encerrado. Quando precisar, é só chamar!");
                            }
                        },
                        () -> whatsAppService.enviarMensagem(
                                atendimento.getTelefone(), MSG_OPCAO_INVALIDA_CONFIRMACAO));
    }

    private void fluxoEscolhaTipo(Atendimento atendimento, String mensagem) {
        Optional<Integer> opcaoOpt = parseOpcaoUmOuDois(mensagem);
        if (opcaoOpt.isEmpty()) {
            whatsAppService.enviarMensagem(atendimento.getTelefone(), MSG_OPCAO_INVALIDA_TIPO);
            return;
        }
        int opcao = opcaoOpt.get();
        if (opcao == 1) {
            clienteService.obterOuCriarPorTelefone(atendimento.getTelefone());
            pedidoService
                    .buscarUltimoPedidoPorTelefone(atendimento.getTelefone())
                    .filter(p -> !p.getItens().isEmpty())
                    .ifPresentOrElse(
                            ultimo -> {
                                Pedido novo = pedidoService.repetirPedido(ultimo);
                                montarPayloadOmieMock(novo);
                                atendimentoService.atualizarStatus(atendimento, StatusAtendimento.FINALIZADO);
                                whatsAppService.enviarMensagem(
                                        atendimento.getTelefone(), "✅ Pedido repetido com sucesso!");
                            },
                            () -> whatsAppService.enviarMensagem(
                                    atendimento.getTelefone(), MSG_SEM_PEDIDO_PARA_REPETIR));
            return;
        }
        atendimentoService.atualizarStatus(atendimento, StatusAtendimento.AGUARDANDO_PRODUTO);
        whatsAppService.enviarMensagem(
                atendimento.getTelefone(),
                "Perfeito! Vamos montar seu pedido.\n\nEm breve vou te mostrar os produtos disponíveis.");
        enviarCatalogo(atendimento.getTelefone());
    }

    private static Optional<Integer> parseOpcaoUmOuDois(String mensagem) {
        if (mensagem == null) {
            return Optional.empty();
        }
        String m = mensagem.trim();
        if ("1".equals(m)) {
            return Optional.of(1);
        }
        if ("2".equals(m)) {
            return Optional.of(2);
        }
        return Optional.empty();
    }

    private void processarAguardandoProduto(Atendimento atendimento, String mensagem) {
        Optional<ProdutoCatalogoDto> produto = produtoService.listarProdutos().stream()
                .filter(p -> p.getCodigo().equalsIgnoreCase(mensagem))
                .findFirst();
        if (produto.isEmpty()) {
            whatsAppService.enviarMensagem(
                    atendimento.getTelefone(), "Código inválido. Envie um dos códigos do catálogo.");
            return;
        }
        Cliente cliente = clienteService.obterOuCriarPorTelefone(atendimento.getTelefone());
        Long pedidoId = obterOuCriarPedidoId(atendimento, cliente.getId());
        String codigo = produto.get().getCodigo();
        atendimento.setContextoJson(contextoPedidoECodigo(pedidoId, codigo));
        atendimentoService.salvar(atendimento);
        atendimentoService.atualizarStatus(atendimento, StatusAtendimento.AGUARDANDO_QUANTIDADE);
        whatsAppService.enviarMensagem(
                atendimento.getTelefone(), "Quantas unidades de " + produto.get().getDescricao() + "?");
    }

    private void processarAguardandoQuantidade(Atendimento atendimento, String mensagem) {
        int qtd;
        try {
            qtd = Integer.parseInt(mensagem.replaceAll("\\D", ""));
            if (qtd <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            whatsAppService.enviarMensagem(atendimento.getTelefone(), "Informe uma quantidade numérica válida.");
            return;
        }
        Long pedidoId = extrairPedidoId(atendimento.getContextoJson())
                .orElseThrow(() -> new IllegalStateException("contexto sem pedidoId"));
        String codigo = extrairCodigoProduto(atendimento.getContextoJson())
                .orElseThrow(() -> new IllegalStateException("contexto sem codigoProduto"));
        ProdutoCatalogoDto produto = produtoService.listarProdutos().stream()
                .filter(p -> p.getCodigo().equalsIgnoreCase(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("produto não encontrado no catálogo: " + codigo));

        pedidoService.adicionarItem(
                pedidoId, produto.getCodigo(), produto.getDescricao(), qtd, produto.getValorUnitario());
        atendimentoService.atualizarStatus(atendimento, StatusAtendimento.AGUARDANDO_CONFIRMACAO_PEDIDO);
        whatsAppService.enviarMensagem(
                atendimento.getTelefone(),
                "Item adicionado. Digite CONFIRMAR para enviar o pedido ou MAIS para incluir outro produto.");
    }

    private void processarAguardandoConfirmacaoPedido(Atendimento atendimento, String mensagem) {
        String m = mensagem.toLowerCase();
        if (m.contains("mais")) {
            atendimentoService.atualizarStatus(atendimento, StatusAtendimento.AGUARDANDO_PRODUTO);
            enviarCatalogo(atendimento.getTelefone());
            return;
        }
        if (respostaSim(mensagem) || m.contains("confirmar")) {
            Long pedidoId = extrairPedidoId(atendimento.getContextoJson())
                    .orElseThrow(() -> new IllegalStateException("contexto sem pedidoId"));
            Pedido pedido = pedidoService.finalizarPedido(pedidoId);
            montarPayloadOmieMock(pedido);
            atendimentoService.atualizarStatus(atendimento, StatusAtendimento.FINALIZADO);
            whatsAppService.enviarMensagem(
                    atendimento.getTelefone(), "Pedido registrado. Obrigado pela preferência!");
            return;
        }
        whatsAppService.enviarMensagem(
                atendimento.getTelefone(), "Responda CONFIRMAR para finalizar ou MAIS para outro item.");
    }

    private void processarFinalizado(Atendimento atendimento, String mensagem) {
        whatsAppService.enviarMensagem(
                atendimento.getTelefone(), "Este atendimento já foi finalizado. Inicie uma nova conversa.");
    }

    /**
     * Com {@link AtendimentoService#buscarOuCriarPorTelefone}, atendimentos expirados geram nova sessão;
     * este ramo permanece para evolução (ex.: reutilizar mesmo registro) ou chamadas internas futuras.
     */
    private void processarExpirado(Atendimento atendimento, String mensagem) {
        whatsAppService.enviarMensagem(
                atendimento.getTelefone(),
                "Este atendimento expirou. Você já está em uma nova sessão; responda 1 para continuar.");
    }

    private void enviarCatalogo(String telefone) {
        List<ProdutoCatalogoDto> lista = produtoService.listarProdutos();
        StringBuilder sb = new StringBuilder("Catálogo (envie o código):\n");
        lista.forEach(p -> sb.append(p.getCodigo())
                .append(" - ")
                .append(p.getDescricao())
                .append(" (R$ ")
                .append(p.getValorUnitario())
                .append(")\n"));
        whatsAppService.enviarMensagem(telefone, sb.toString().trim());
    }

    private Long obterOuCriarPedidoId(Atendimento atendimento, Long clienteId) {
        return extrairPedidoId(atendimento.getContextoJson())
                .orElseGet(() -> pedidoService.criarPedido(clienteId).getId());
    }

    private String contextoPedidoECodigo(Long pedidoId, String codigoProduto) {
        return "{\"pedidoId\":" + pedidoId + ",\"codigoProduto\":\"" + codigoProduto + "\"}";
    }

    private Optional<Long> extrairPedidoId(String contextoJson) {
        if (contextoJson == null) {
            return Optional.empty();
        }
        Matcher m = PEDIDO_ID_PATTERN.matcher(contextoJson);
        if (m.find()) {
            return Optional.of(Long.parseLong(m.group(1)));
        }
        return Optional.empty();
    }

    private Optional<String> extrairCodigoProduto(String contextoJson) {
        if (contextoJson == null) {
            return Optional.empty();
        }
        Matcher m = CODIGO_PRODUTO_PATTERN.matcher(contextoJson);
        if (m.find()) {
            return Optional.of(m.group(1));
        }
        return Optional.empty();
    }

    private void montarPayloadOmieMock(Pedido pedido) {
        List<OmiePedidoItemRequest> itens = pedido.getItens().stream()
                .map(i -> new OmiePedidoItemRequest(
                        i.getCodigoProduto(),
                        i.getQuantidade(),
                        i.getValorUnitario(),
                        i.getDescricao()))
                .toList();
        OmiePedidoCompraRequest req = new OmiePedidoCompraRequest(
                String.valueOf(pedido.getCliente().getId()),
                "Pedido WhatsApp #" + pedido.getId(),
                itens);
        omieService.criarPedidoOmie(req);
    }

    private boolean respostaSim(String mensagem) {
        String m = mensagem.toLowerCase();
        return m.equals("sim") || m.equals("s") || m.equals("ok") || m.equals("👍");
    }

}
