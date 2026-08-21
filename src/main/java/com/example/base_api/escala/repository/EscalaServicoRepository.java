package com.example.base_api.escala.repository;

import com.example.base_api.escala.entity.EscalaServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EscalaServicoRepository extends JpaRepository<EscalaServico, UUID> {

    List<EscalaServico> findByEscalaId(UUID escalaId);

    Optional<EscalaServico> findByEscalaIdAndServicoId(UUID escalaId, UUID servicoId);

    void deleteByEscalaId(UUID escalaId);
}
