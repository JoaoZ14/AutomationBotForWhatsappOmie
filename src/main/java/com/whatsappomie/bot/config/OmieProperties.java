package com.whatsappomie.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.omie")
public class OmieProperties {

    /** Quando true e credenciais preenchidas, envia HTTP real. */
    private boolean enabled = false;

    private String appKey = "";
    private String appSecret = "";
    private String baseUrl = "https://app.omie.com.br/api/v1";
    private String pedidoPath = "/produtos/pedido/";
    private String pedidoCompraPath = "/produtos/pedidocompra/";
    private String produtosPath = "/geral/produtos/";

    private final Pedido pedido = new Pedido();
    private final PedidoCompra pedidoCompra = new PedidoCompra();

    public boolean isCredenciaisPreenchidas() {
        return enabled && !appKey.isBlank() && !appSecret.isBlank();
    }

    public boolean isChamadaHttpAtiva() {
        return enabled
                && !appKey.isBlank()
                && !appSecret.isBlank();
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
        this.appKey = appKey != null ? appKey.trim() : "";
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret != null ? appSecret.trim() : "";
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPedidoPath() {
        return pedidoPath;
    }

    public void setPedidoPath(String pedidoPath) {
        this.pedidoPath = pedidoPath;
    }

    public Pedido getPedido() {
        return pedido;
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

    public String urlPedido() {
        return buildUrl(pedidoPath);
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

    public static class Pedido {
        /** Código fixo de cliente Omie (opcional, uso temporário). */
        private String codigoCliente = "";
        /** Etapa do pedido de venda Omie. */
        private String etapa = "10";
        /** Código da parcela Omie. */
        private String codigoParcela = "999";
        /** Código da categoria financeira Omie (opcional). */
        private String codigoCategoria = "";
        /** Conta corrente Omie (opcional). */
        private String contaCorrenteCodigo = "";

        public String getCodigoCliente() {
            return codigoCliente;
        }

        public void setCodigoCliente(String codigoCliente) {
            this.codigoCliente = codigoCliente;
        }

        public String getEtapa() {
            return etapa;
        }

        public void setEtapa(String etapa) {
            this.etapa = etapa;
        }

        public String getCodigoParcela() {
            return codigoParcela;
        }

        public void setCodigoParcela(String codigoParcela) {
            this.codigoParcela = codigoParcela;
        }

        public String getCodigoCategoria() {
            return codigoCategoria;
        }

        public void setCodigoCategoria(String codigoCategoria) {
            this.codigoCategoria = codigoCategoria;
        }

        public String getContaCorrenteCodigo() {
            return contaCorrenteCodigo;
        }

        public void setContaCorrenteCodigo(String contaCorrenteCodigo) {
            this.contaCorrenteCodigo = contaCorrenteCodigo;
        }
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
