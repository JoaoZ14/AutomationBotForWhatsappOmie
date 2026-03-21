package com.whatsappomie.bot.dto.omie;

import java.util.List;

/**
 * Estrutura base para futura chamada à API Omie (pedido de compra / pedido de venda conforme contrato real).
 */
public class OmiePedidoCompraRequest {

    private String codigoClienteOmie;
    private String observacoes;
    private List<OmiePedidoItemRequest> itens;

    public OmiePedidoCompraRequest() {}

    public OmiePedidoCompraRequest(String codigoClienteOmie, String observacoes, List<OmiePedidoItemRequest> itens) {
        this.codigoClienteOmie = codigoClienteOmie;
        this.observacoes = observacoes;
        this.itens = itens;
    }

    public String getCodigoClienteOmie() {
        return codigoClienteOmie;
    }

    public void setCodigoClienteOmie(String codigoClienteOmie) {
        this.codigoClienteOmie = codigoClienteOmie;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public List<OmiePedidoItemRequest> getItens() {
        return itens;
    }

    public void setItens(List<OmiePedidoItemRequest> itens) {
        this.itens = itens;
    }
}
