package com.example.base_api.usuario.repository;

import com.example.base_api.usuario.entity.Papel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PapelRepository extends JpaRepository<Papel, UUID> {

    /** Papel de sistema (global). */
    Optional<Papel> findByNomeAndIgrejaIdIsNull(String nome);

    /** Papel customizado de uma igreja específica. */
    Optional<Papel> findByNomeAndIgrejaId(String nome, UUID igrejaId);

    boolean existsByNomeAndIgrejaId(String nome, UUID igrejaId);

    /** Papéis visíveis para uma igreja: os de sistema + os customizados dela. */
    List<Papel> findByIgrejaIdIsNullOrIgrejaId(UUID igrejaId);
}
