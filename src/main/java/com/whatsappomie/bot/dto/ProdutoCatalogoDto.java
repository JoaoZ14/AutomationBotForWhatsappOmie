package com.whatsappomie.bot.dto;

import java.math.BigDecimal;

public class ProdutoCatalogoDto {

    private String codigo;
    /** Código usado na API Omie (codigo_produto). Se vazio, usa {@link #codigo}. */
    private String codigoProdutoOmie;
    private String descricao;
    private BigDecimal valorUnitario;

    public ProdutoCatalogoDto() {}

    public ProdutoCatalogoDto(String codigo, String descricao, BigDecimal valorUnitario) {
        this(codigo, codigo, descricao, valorUnitario);
    }

    public ProdutoCatalogoDto(String codigo, String codigoProdutoOmie, String descricao, BigDecimal valorUnitario) {
        this.codigo = codigo;
        this.codigoProdutoOmie = codigoProdutoOmie != null ? codigoProdutoOmie : codigo;
        this.descricao = descricao;
        this.valorUnitario = valorUnitario != null ? valorUnitario : BigDecimal.ZERO;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigoProdutoOmie() {
        if (codigoProdutoOmie != null && !codigoProdutoOmie.isBlank()) {
            return codigoProdutoOmie;
        }
        return codigo != null ? codigo : "";
    }

    public void setCodigoProdutoOmie(String codigoProdutoOmie) {
        this.codigoProdutoOmie = codigoProdutoOmie;
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
