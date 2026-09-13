package com.example.base_api.servico.repository;

import com.example.base_api.servico.entity.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicoRepository extends JpaRepository<Servico, UUID> {

    List<Servico> findByMinisterioIdOrderByNomeAsc(UUID ministerioId);

    List<Servico> findByMinisterioIdIn(List<UUID> ministerioIds);

    List<Servico> findByMinisterioIdInAndAtivoTrueOrderByNomeAsc(List<UUID> ministerioIds);

    long countByMinisterioIdIn(List<UUID> ministerioIds);

    Optional<Servico> findByMinisterioIdAndNomeIgnoreCase(UUID ministerioId, String nome);

    boolean existsByMinisterioIdAndNomeIgnoreCaseAndIdNot(UUID ministerioId, String nome, UUID id);
}
