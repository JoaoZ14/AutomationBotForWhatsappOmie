package com.whatsappomie.bot.service;

import com.whatsappomie.bot.domain.Cliente;
import com.whatsappomie.bot.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Cliente obterOuCriarPorTelefone(String telefone) {
        return clienteRepository
                .findByTelefone(telefone)
                .orElseGet(() -> {
                    Cliente c = new Cliente();
                    c.setTelefone(telefone);
                    c.setNome("Cliente " + telefone);
                    c.setAtivo(true);
                    return clienteRepository.save(c);
                });
    }
}
