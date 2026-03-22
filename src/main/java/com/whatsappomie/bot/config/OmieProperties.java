package com.whatsappomie.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.omie")
public class OmieProperties {

    /** Quando true e credenciais/pedido-compra preenchidos, envia HTTP real. */
    private boolean enabled = false;

    private String appKey = "";
    private String appSecret = "";
    private String baseUrl = "https://app.omie.com.br/api/v1";
    private String pedidoCompraPath = "/produtos/pedidocompra/";
    private String produtosPath = "/geral/produtos/";

    private final PedidoCompra pedidoCompra = new PedidoCompra();

    public boolean isCredenciaisPreenchidas() {
        return enabled && !appKey.isBlank() && !appSecret.isBlank();
    }

    public boolean isChamadaHttpAtiva() {
        return enabled
                && !appKey.isBlank()
                && !appSecret.isBlank()
                && !pedidoCompra.getFornecedorCodigo().isBlank();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPedidoCompraPath() {
        return pedidoCompraPath;
    }

    public void setPedidoCompraPath(String pedidoCompraPath) {
        this.pedidoCompraPath = pedidoCompraPath;
    }

    public PedidoCompra getPedidoCompra() {
        return pedidoCompra;
    }

    public String urlPedidoCompra() {
        return buildUrl(pedidoCompraPath);
    }

    public String urlProdutos() {
        return buildUrl(produtosPath);
    }

    private String buildUrl(String pathSegment) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = pathSegment.startsWith("/") ? pathSegment : "/" + pathSegment;
        return base + path;
    }

    public String getProdutosPath() {
        return produtosPath;
    }

    public void setProdutosPath(String produtosPath) {
        this.produtosPath = produtosPath;
    }

    public static class PedidoCompra {

        /**
         * Código do fornecedor no Omie ({@code nCodFor} em {@code cabecalho_incluir}).
         * Obrigatório para chamada real de pedido de compra.
         */
        private String fornecedorCodigo = "";

        /**
         * Conta corrente no Omie ({@code nCodCC}); incluído no JSON somente se não vazio.
         */
        private String contaCorrenteCodigo = "";

        public String getFornecedorCodigo() {
            return fornecedorCodigo;
        }

        public void setFornecedorCodigo(String fornecedorCodigo) {
            this.fornecedorCodigo = fornecedorCodigo;
        }

        public String getContaCorrenteCodigo() {
            return contaCorrenteCodigo;
        }

        public void setContaCorrenteCodigo(String contaCorrenteCodigo) {
            this.contaCorrenteCodigo = contaCorrenteCodigo;
        }
    }
}
