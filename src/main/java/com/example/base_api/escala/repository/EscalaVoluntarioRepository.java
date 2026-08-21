package com.example.base_api.escala.repository;

import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.entity.StatusEscalaVoluntario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EscalaVoluntarioRepository extends JpaRepository<EscalaVoluntario, UUID> {

    List<EscalaVoluntario> findByEscalaId(UUID escalaId);

    List<EscalaVoluntario> findByPessoaId(UUID pessoaId);

    Optional<EscalaVoluntario> findByEscalaIdAndPessoaIdAndServicoId(
            UUID escalaId,
            UUID pessoaId,
            UUID servicoId
    );

    long countByEscalaIdAndServicoIdAndStatusIn(
            UUID escalaId,
            UUID servicoId,
            List<StatusEscalaVoluntario> status
    );
}
