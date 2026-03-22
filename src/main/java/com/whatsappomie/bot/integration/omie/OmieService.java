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
                    "[Omie mock] Payload preparado: codigoClienteOmie={}, itens={} (HTTP desligado: app.omie.enabled=false ou faltam credenciais/código fornecedor)",
                    request.getCodigoClienteOmie(),
                    request.getItens() != null ? request.getItens().size() : 0);
            return;
        }

        Map<String, Object> body = montarCorpoIncluirPedCompra(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = properties.urlPedidoCompra();

        try {
            log.info("[Omie] POST {} call=IncluirPedCompra cCodIntPed no param", url);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            log.info("[Omie] Resposta IncluirPedCompra status={} body={}", resp.getStatusCode(), resp.getBody());
        } catch (HttpStatusCodeException e) {
            log.error(
                    "[Omie] Erro IncluirPedCompra status={} body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new IllegalStateException(
                    "Falha ao incluir pedido de compra na Omie: HTTP " + e.getStatusCode(), e);
        }
    }

    private Map<String, Object> montarCorpoIncluirPedCompra(OmiePedidoCompraRequest request) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("call", "IncluirPedCompra");
        root.put("app_key", properties.getAppKey());
        root.put("app_secret", properties.getAppSecret());
        root.put("param", List.of(montarParamIncluir(request)));
        return root;
    }

    private Map<String, Object> montarParamIncluir(OmiePedidoCompraRequest request) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("cabecalho_incluir", montarCabecalhoIncluir(request));
        param.put("frete_incluir", montarFreteIncluir());
        param.put("produtos_incluir", montarProdutosIncluir(request.getItens()));
        return param;
    }

    private Map<String, Object> montarCabecalhoIncluir(OmiePedidoCompraRequest request) {
        Map<String, Object> cab = new LinkedHashMap<>();
        cab.put("cCodIntPed", codigoIntegracaoPedido(request.getObservacoes()));
        cab.put("dDtPrevisao", LocalDate.now().plusDays(7).format(DATA_PREVISAO));
        cab.put("cCodParc", "999");
        cab.put("nQtdeParc", 1);
        cab.put("nCodFor", properties.getPedidoCompra().getFornecedorCodigo());
        cab.put("cCodIntFor", "");
        cab.put("cCodCateg", "");
        cab.put("nCodCompr", 0);
        cab.put("cContato", "");
        cab.put("cContrato", "");
        String cc = properties.getPedidoCompra().getContaCorrenteCodigo();
        if (cc != null && !cc.isBlank()) {
            cab.put("nCodCC", cc);
        } else {
            cab.put("nCodCC", 0);
        }
        cab.put("nCodIntCC", 0);
        cab.put("nCodProj", 0);
        cab.put("cNumPedido", "");
        cab.put("cObs", request.getObservacoes() != null ? request.getObservacoes() : "");
        cab.put("cObsInt", "Pedido via WhatsApp (automation-bot)");
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
        f.put("nCodTransp", 0);
        f.put("cCodIntTransp", "");
        f.put("cTpFrete", "9");
        f.put("cPlaca", "");
        f.put("cUF", "");
        f.put("nQtdVol", 0);
        f.put("cEspVol", "");
        f.put("cMarVol", "");
        f.put("cNumVol", "");
        f.put("nPesoLiq", 0.0);
        f.put("nPesoBruto", 0.0);
        f.put("nValFrete", 0.0);
        f.put("nValSeguro", 0.0);
        f.put("cLacre", "");
        f.put("nValOutras", 0.0);
        return f;
    }

    private static List<Map<String, Object>> montarProdutosIncluir(List<OmiePedidoItemRequest> itens) {
        List<Map<String, Object>> lista = new ArrayList<>();
        if (itens == null) {
            return lista;
        }
        int seq = 1;
        for (OmiePedidoItemRequest item : itens) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("cCodIntItem", "ITEM" + seq++);
            p.put("cCodIntProd", "");
            p.put("nCodProd", item.getCodigoProduto() != null ? item.getCodigoProduto() : "");
            p.put("cProduto", "");
            p.put("cDescricao", item.getDescricao() != null ? item.getDescricao() : "");
            p.put("cNCM", "");
            p.put("cUnidade", "");
            p.put("cEAN", "");
            p.put("nPesoLiq", 0);
            p.put("nPesoBruto", 0);
            Integer qtd = item.getQuantidade();
            p.put("nQtde", qtd != null ? qtd : 0);
            p.put("nValUnit", valorUnitarioOmie(item.getValorUnitario()));
            p.put("nDesconto", 0.0);
            p.put("nValorIcms", 0.0);
            p.put("nValorSt", 0.0);
            p.put("nValorIpi", 0.0);
            p.put("nValorPis", 0.0);
            p.put("nValorCofins", 0.0);
            p.put("cObs", "");
            p.put("cMkpAtuPv", "N");
            p.put("cMkpAtuSm", "N");
            p.put("nMkpPerc", 0);
            p.put("codigo_local_estoque", "");
            p.put("cCodCateg", "");
            lista.add(p);
        }
        return lista;
    }

    private static double valorUnitarioOmie(BigDecimal valor) {
        if (valor == null) {
            return 0.0;
        }
        return valor.doubleValue();
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
        headers.setContentType(MediaType.APPLICATION_JSON);
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
            log.error("[Omie] Erro ListarProdutos status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
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
            String descricao = stringOrEmpty(prod.get("descricao"));
            BigDecimal valor = toBigDecimal(prod.get("valor_unitario"));
            if (!codigo.isBlank() || !descricao.isBlank()) {
                resultado.add(new ProdutoCatalogoDto(codigo, codigoProdutoOmie, descricao, valor));
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
            return new BigDecimal(o.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
