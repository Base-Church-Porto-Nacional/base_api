package com.example.base_api.escala.repository;

import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.entity.StatusEscalaVoluntario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EscalaVoluntarioRepository extends JpaRepository<EscalaVoluntario, UUID> {

    List<EscalaVoluntario> findByEscalaId(UUID escalaId);

    List<EscalaVoluntario> findByEscalaIdIn(List<UUID> escalaIds);

    List<EscalaVoluntario> findByPessoaIdInAndDataItemBetween(
            List<UUID> pessoaIds,
            LocalDate inicio,
            LocalDate fim
    );

    // Lista: com designações por data, a mesma pessoa pode ter várias linhas no mesmo serviço.
    List<EscalaVoluntario> findByEscalaIdAndPessoaIdAndServicoId(
            UUID escalaId,
            UUID pessoaId,
            UUID servicoId
    );

    Optional<EscalaVoluntario> findByEscalaIdAndPessoaIdAndServicoIdAndDataItem(
            UUID escalaId,
            UUID pessoaId,
            UUID servicoId,
            LocalDate dataItem
    );

    long countByEscalaIdAndServicoIdAndDataItemAndStatusIn(
            UUID escalaId,
            UUID servicoId,
            LocalDate dataItem,
            List<StatusEscalaVoluntario> status
    );

    long countByEscalaIdAndServicoIdAndStatusIn(
            UUID escalaId,
            UUID servicoId,
            List<StatusEscalaVoluntario> status
    );
}
