package com.whatsappomie.bot.integration.omie;

import com.whatsappomie.bot.config.OmieProperties;
import com.whatsappomie.bot.dto.ProdutoCatalogoDto;
import com.whatsappomie.bot.dto.omie.OmiePedidoCompraRequest;
import com.whatsappomie.bot.dto.omie.OmiePedidoItemRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class OmieService {

    private static final Logger log = LoggerFactory.getLogger(OmieService.class);
    private static final DateTimeFormatter DATA_PREVISAO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern PEDIDO_WHATSAPP_ID = Pattern.compile("Pedido WhatsApp #(\\d+)");

    private final RestTemplate restTemplate;
    private final OmieProperties properties;

    public OmieService(RestTemplate restTemplate, OmieProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public void criarPedidoOmie(OmiePedidoCompraRequest request) {
        if (!properties.isChamadaHttpAtiva()) {
            log.info(
                    "[Omie mock] Payload preparado: codigoClienteOmie={}, itens={} (HTTP desligado: app.omie.enabled=false ou faltam credenciais)",
                    request.getCodigoClienteOmie(),
                    request.getItens() != null ? request.getItens().size() : 0);
            return;
        }

        Map<String, Object> body = montarCorpoIncluirPedido(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = properties.urlPedido();

        try {
            log.info("[Omie] POST {} call=IncluirPedido", url);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            log.info("[Omie] Resposta IncluirPedido status={} body={}", resp.getStatusCode(), resp.getBody());
        } catch (HttpStatusCodeException e) {
            log.error(
                    "[Omie] Erro IncluirPedido status={} body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new IllegalStateException("Falha ao incluir pedido de venda na Omie: HTTP " + e.getStatusCode(), e);
        }
    }

    private Map<String, Object> montarCorpoIncluirPedido(OmiePedidoCompraRequest request) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("call", "IncluirPedido");
        root.put("app_key", properties.getAppKey());
        root.put("app_secret", properties.getAppSecret());
        root.put("param", List.of(montarParamIncluir(request)));
        return root;
    }

    private Map<String, Object> montarParamIncluir(OmiePedidoCompraRequest request) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("cabecalho", montarCabecalhoIncluir(request));
        param.put("det", montarDetalhesItens(request.getItens()));
        param.put("frete", montarFreteIncluir());
        param.put("informacoes_adicionais", montarInformacoesAdicionais());
        return param;
    }

    private Map<String, Object> montarCabecalhoIncluir(OmiePedidoCompraRequest request) {
        Map<String, Object> cab = new LinkedHashMap<>();
        cab.put("codigo_cliente", toLongOrZero(request.getCodigoClienteOmie()));
        cab.put("codigo_pedido_integracao", codigoIntegracaoPedido(request.getObservacoes()));
        cab.put("data_previsao", LocalDate.now().plusDays(7).format(DATA_PREVISAO));
        cab.put("etapa", valorOuPadrao(properties.getPedido().getEtapa(), "10"));
        cab.put("numero_pedido", "");
        cab.put("codigo_parcela", valorOuPadrao(properties.getPedido().getCodigoParcela(), "999"));
        cab.put("quantidade_itens", request.getItens() != null ? request.getItens().size() : 0);
        return cab;
    }

    private static String codigoIntegracaoPedido(String observacoes) {
        if (observacoes != null) {
            Matcher m = PEDIDO_WHATSAPP_ID.matcher(observacoes);
            if (m.find()) {
                return "WA-" + m.group(1);
            }
        }
        return "WA-" + System.currentTimeMillis();
    }

    private static Map<String, Object> montarFreteIncluir() {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("modalidade", "9");
        return f;
    }

    private List<Map<String, Object>> montarDetalhesItens(List<OmiePedidoItemRequest> itens) {
        List<Map<String, Object>> lista = new ArrayList<>();
        if (itens == null) {
            return lista;
        }
        int seq = 1;
        for (OmiePedidoItemRequest item : itens) {
            Map<String, Object> ide = new LinkedHashMap<>();
            ide.put("codigo_item_integracao", "ITEM" + seq++);

            Map<String, Object> produto = new LinkedHashMap<>();
            produto.put("codigo_produto", item.getCodigoProduto() != null ? item.getCodigoProduto() : "");
            produto.put("descricao", item.getDescricao() != null ? item.getDescricao() : "");
            Integer qtd = item.getQuantidade();
            produto.put("quantidade", qtd != null ? qtd : 0);
            produto.put("valor_unitario", valorUnitarioOmie(item.getValorUnitario()));
            produto.put("tipo_desconto", "V");
            produto.put("valor_desconto", 0.0);
            produto.put("unidade", "UN");

            Map<String, Object> det = new LinkedHashMap<>();
            det.put("ide", ide);
            det.put("produto", produto);
            lista.add(det);
        }
        return lista;
    }

    private Map<String, Object> montarInformacoesAdicionais() {
        Map<String, Object> info = new LinkedHashMap<>();
        String categoria = properties.getPedido().getCodigoCategoria();
        if (categoria != null && !categoria.isBlank()) {
            info.put("codigo_categoria", categoria);
        }
        String contaCorrente = properties.getPedido().getContaCorrenteCodigo();
        if (contaCorrente != null && !contaCorrente.isBlank()) {
            info.put("codigo_conta_corrente", toLongOrZero(contaCorrente));
        }
        info.put("consumidor_final", "S");
        info.put("enviar_email", "N");
        return info;
    }

    private static double valorUnitarioOmie(BigDecimal valor) {
        if (valor == null) {
            return 0.0;
        }
        return valor.doubleValue();
    }

    private static long toLongOrZero(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static String valorOuPadrao(String valor, String padrao) {
        if (valor == null || valor.isBlank()) {
            return padrao;
        }
        return valor;
    }

    /**
     * Lista produtos cadastrados na Omie via API ListarProdutos.
     * Retorna lista vazia se Omie desabilitado ou credenciais ausentes.
     */
    public List<ProdutoCatalogoDto> listarProdutos(int pagina, int registrosPorPagina) {
        if (!properties.isCredenciaisPreenchidas()) {
            log.debug("[Omie] ListarProdutos não executado: credenciais ausentes ou enabled=false");
            return Collections.emptyList();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("call", "ListarProdutos");
        body.put("app_key", properties.getAppKey());
        body.put("app_secret", properties.getAppSecret());
        body.put(
                "param",
                List.of(Map.of(
                        "pagina", pagina,
                        "registros_por_pagina", registrosPorPagina,
                        "apenas_importado_api", "N",
                        "filtrar_apenas_omiepdv", "N")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/json; charset=UTF-8"));
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = properties.urlProdutos();

        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            return extrairProdutosDaResposta(resp.getBody());
        } catch (HttpStatusCodeException e) {
            log.error(
                    "[Omie] Erro ListarProdutos status={} body={} (appKey presente: {})",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    !properties.getAppKey().isBlank());
            return Collections.emptyList();
        }
    }

    /**
     * Lista pedidos de venda cadastrados na Omie via API ListarPedidos.
     * Retorna lista vazia se Omie desabilitado, credenciais ausentes ou erro HTTP.
     */
    public List<Map<String, Object>> listarPedidos(int pagina, int registrosPorPagina) {
        if (!properties.isCredenciaisPreenchidas()) {
            log.debug("[Omie] ListarPedidos não executado: credenciais ausentes ou enabled=false");
            return Collections.emptyList();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("call", "ListarPedidos");
        body.put("app_key", properties.getAppKey());
        body.put("app_secret", properties.getAppSecret());
        body.put(
                "param",
                List.of(Map.of(
                        "pagina", pagina,
                        "registros_por_pagina", registrosPorPagina,
                        "apenas_importado_api", "N")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/json; charset=UTF-8"));
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = properties.urlPedido();

        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            return extrairPedidosDaResposta(resp.getBody());
        } catch (HttpStatusCodeException e) {
            log.error(
                    "[Omie] Erro ListarPedidos status={} body={} (appKey presente: {})",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    !properties.getAppKey().isBlank());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ProdutoCatalogoDto> extrairProdutosDaResposta(Map<String, Object> body) {
        if (body == null) {
            return Collections.emptyList();
        }
        Object arr = body.get("produto_servico_cadastro");
        if (!(arr instanceof List<?>)) {
            return Collections.emptyList();
        }
        List<?> lista = (List<?>) arr;
        List<ProdutoCatalogoDto> resultado = new ArrayList<>();
        for (Object item : lista) {
            if (!(item instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> prod = (Map<String, Object>) item;
            String codigo = stringOrEmpty(prod.get("codigo"));
            Object codigoProdutoObj = prod.get("codigo_produto");
            String codigoProdutoOmie = codigoProdutoObj != null ? String.valueOf(codigoProdutoObj) : codigo;
            if (codigo.isBlank()) {
                codigo = codigoProdutoOmie;
            }
            String descricao = stringOrEmpty(prod.get("descricao"));
            BigDecimal valor = toBigDecimal(prod.get("valor_unitario"));
            if (!codigo.isBlank() || !descricao.isBlank()) {
                resultado.add(new ProdutoCatalogoDto(codigo, codigoProdutoOmie, descricao, valor));
            }
        }
        return resultado;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extrairPedidosDaResposta(Map<String, Object> body) {
        if (body == null) {
            return Collections.emptyList();
        }

        Object arr = body.get("pedido_venda_produto");
        if (!(arr instanceof List<?>)) {
            arr = body.get("pedidos");
        }
        if (!(arr instanceof List<?>)) {
            arr = body.get("pedido");
        }
        if (!(arr instanceof List<?> lista)) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Object item : lista) {
            if (item instanceof Map<?, ?> mapItem) {
                Map<String, Object> pedido = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : mapItem.entrySet()) {
                    if (entry.getKey() != null) {
                        pedido.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                resultado.add(pedido);
            }
        }
        return resultado;
    }

    private static String stringOrEmpty(Object o) {
        return o != null ? o.toString().trim() : "";
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        if (o instanceof Number) {
            return BigDecimal.valueOf(((Number) o).doubleValue());
        }
        try {
            String valor = o.toString().trim();
            if (valor.isEmpty()) {
                return BigDecimal.ZERO;
            }
            // Omie pode retornar decimal com vírgula (pt-BR) ou ponto.
            if (valor.contains(",")) {
                valor = valor.replace(".", "").replace(",", ".");
            }
            return new BigDecimal(valor);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
