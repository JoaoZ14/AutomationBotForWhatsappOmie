package com.whatsappomie.bot.repository;

import com.whatsappomie.bot.domain.Atendimento;
import com.whatsappomie.bot.domain.StatusAtendimento;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    Optional<Atendimento> findFirstByTelefoneOrderByIdDesc(String telefone);

    List<Atendimento> findByExpiraEmBeforeAndStatusNotIn(Instant agora, List<StatusAtendimento> statusFinais);

    List<Atendimento> findByStatusInAndUltimoContatoBeforeAndTentativasLessThan(
            List<StatusAtendimento> status,
            Instant antesDe,
            int maxTentativas);
}
