package com.whatsappomie.bot.service;

import com.whatsappomie.bot.dto.ProdutoCatalogoDto;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    public List<ProdutoCatalogoDto> listarProdutos() {
        return List.of(
                new ProdutoCatalogoDto("SKU001", "Produto exemplo A", new BigDecimal("10.50")),
                new ProdutoCatalogoDto("SKU002", "Produto exemplo B", new BigDecimal("22.00")));
    }
}
