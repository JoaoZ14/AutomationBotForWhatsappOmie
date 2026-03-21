package com.whatsappomie.bot.dto.omie;

import java.math.BigDecimal;

public class OmiePedidoItemRequest {

    private String codigoProduto;
    private Integer quantidade;
    private BigDecimal valorUnitario;
    private String descricao;

    public OmiePedidoItemRequest() {}

    public OmiePedidoItemRequest(
            String codigoProduto, Integer quantidade, BigDecimal valorUnitario, String descricao) {
        this.codigoProduto = codigoProduto;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
        this.descricao = descricao;
    }

    public String getCodigoProduto() {
        return codigoProduto;
    }

    public void setCodigoProduto(String codigoProduto) {
        this.codigoProduto = codigoProduto;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
