package com.example.base_api.ministerio.repository;

import com.example.base_api.ministerio.entity.Ministerio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MinisterioRepository extends JpaRepository<Ministerio, UUID> {

    Optional<Ministerio> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);
}
