package com.whatsappomie.bot.domain;

/**
 * Ciclo de vida do pedido no sistema (evolução futura: envio à Omie, cancelamento, etc.).
 */
public enum StatusPedido {
    ABERTO,
    FINALIZADO,
    PENDENTE_ENVIO_OMIE
}
