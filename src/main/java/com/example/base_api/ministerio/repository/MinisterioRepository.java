package com.example.base_api.ministerio.repository;

import com.example.base_api.ministerio.entity.Ministerio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MinisterioRepository extends JpaRepository<Ministerio, UUID> {

    Optional<Ministerio> findByIgrejaIdAndNomeIgnoreCase(UUID igrejaId, String nome);

    boolean existsByIgrejaIdAndNomeIgnoreCaseAndIdNot(UUID igrejaId, String nome, UUID id);

    List<Ministerio> findByIgrejaIdOrderByNomeAsc(UUID igrejaId);

    List<Ministerio> findByIgrejaIdInOrderByNomeAsc(List<UUID> igrejaIds);

    long countByIgrejaIdIn(List<UUID> igrejaIds);
}
