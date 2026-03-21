package com.whatsappomie.bot.service;

import com.whatsappomie.bot.domain.Cliente;
import com.whatsappomie.bot.domain.ItemPedido;
import com.whatsappomie.bot.domain.Pedido;
import com.whatsappomie.bot.domain.StatusPedido;
import com.whatsappomie.bot.repository.ClienteRepository;
import com.whatsappomie.bot.repository.PedidoRepository;
import com.whatsappomie.bot.util.TelefoneNormalizer;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Pedido criarPedido(Long clienteId) {
        Cliente cliente = clienteRepository
                .findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + clienteId));
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.ABERTO);
        pedido.setCriadoEm(Instant.now());
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido adicionarItem(
            Long pedidoId,
            String codigoProduto,
            String descricao,
            int quantidade,
            BigDecimal valorUnitario) {
        Pedido pedido = pedidoRepository
                .findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + pedidoId));
        ItemPedido item = new ItemPedido();
        item.setPedido(pedido);
        item.setCodigoProduto(codigoProduto);
        item.setDescricao(descricao);
        item.setQuantidade(quantidade);
        item.setValorUnitario(valorUnitario);
        pedido.getItens().add(item);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido finalizarPedido(Long pedidoId) {
        Pedido pedido = pedidoRepository
                .findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + pedidoId));
        pedido.setStatus(StatusPedido.FINALIZADO);
        return pedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public Pedido buscarPorId(Long pedidoId) {
        return pedidoRepository
                .findByIdComItens(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + pedidoId));
    }

    /**
     * Último pedido finalizado do cliente identificado pelo telefone. Vazio se não houver cliente ou histórico.
     */
    @Transactional(readOnly = true)
    public Optional<Pedido> buscarUltimoPedidoPorTelefone(String telefone) {
        return clienteRepository
                .findByTelefone(TelefoneNormalizer.paraE164(telefone.trim()))
                .flatMap(c -> pedidoRepository.findFirstByClienteIdAndStatusOrderByCriadoEmDesc(
                        c.getId(), StatusPedido.FINALIZADO));
    }

    /**
     * Cria um novo pedido finalizado com cópia dos itens do pedido de referência (ex.: repetir último pedido).
     */
    @Transactional
    public Pedido repetirPedido(Pedido pedidoOrigem) {
        Long clienteId = pedidoOrigem.getCliente().getId();
        Pedido novo = criarPedido(clienteId);
        for (ItemPedido it : pedidoOrigem.getItens()) {
            adicionarItem(
                    novo.getId(),
                    it.getCodigoProduto(),
                    it.getDescricao(),
                    it.getQuantidade(),
                    it.getValorUnitario());
        }
        Long id = novo.getId();
        finalizarPedido(id);
        return buscarPorId(id);
    }
}
