package com.whatsappomie.bot.repository;

import com.whatsappomie.bot.domain.Pedido;
import com.whatsappomie.bot.domain.StatusPedido;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = "itens")
    Optional<Pedido> findFirstByClienteIdAndStatusOrderByCriadoEmDesc(Long clienteId, StatusPedido status);

    @EntityGraph(attributePaths = "itens")
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> findByIdComItens(@Param("id") Long id);
}
