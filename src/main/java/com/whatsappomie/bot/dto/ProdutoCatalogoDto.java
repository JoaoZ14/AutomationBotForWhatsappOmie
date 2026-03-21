package com.whatsappomie.bot.dto;

import java.math.BigDecimal;

public class ProdutoCatalogoDto {

    private String codigo;
    private String descricao;
    private BigDecimal valorUnitario;

    public ProdutoCatalogoDto() {}

    public ProdutoCatalogoDto(String codigo, String descricao, BigDecimal valorUnitario) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.valorUnitario = valorUnitario;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }
}
