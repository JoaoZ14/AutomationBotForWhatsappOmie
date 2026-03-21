package com.whatsappomie.bot.repository;

import com.whatsappomie.bot.domain.Cliente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByTelefone(String telefone);

    List<Cliente> findByAtivoTrue();
}
