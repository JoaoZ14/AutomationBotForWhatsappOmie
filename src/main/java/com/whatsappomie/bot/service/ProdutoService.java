package com.whatsappomie.bot.service;

import com.whatsappomie.bot.config.OmieProperties;
import com.whatsappomie.bot.dto.ProdutoCatalogoDto;
import com.whatsappomie.bot.integration.omie.OmieService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    private static final int REGISTROS_POR_PAGINA = 50;

    private final OmieService omieService;
    private final OmieProperties omieProperties;

    public ProdutoService(OmieService omieService, OmieProperties omieProperties) {
        this.omieService = omieService;
        this.omieProperties = omieProperties;
    }

    public List<ProdutoCatalogoDto> listarProdutos() {
        if (omieProperties.isCredenciaisPreenchidas()) {
            List<ProdutoCatalogoDto> todos = new ArrayList<>();
            int pagina = 1;
            List<ProdutoCatalogoDto> chunk;
            do {
                chunk = omieService.listarProdutos(pagina, REGISTROS_POR_PAGINA);
                todos.addAll(chunk);
                pagina++;
            } while (chunk.size() >= REGISTROS_POR_PAGINA);
            return todos;
        }
        return List.of(
                new ProdutoCatalogoDto("SKU001", "Produto exemplo A", new BigDecimal("10.50")),
                new ProdutoCatalogoDto("SKU002", "Produto exemplo B", new BigDecimal("22.00")));
    }
}
