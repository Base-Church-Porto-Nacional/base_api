package com.example.base_api.usuario.repository;

import com.example.base_api.usuario.entity.Papel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PapelRepository extends JpaRepository<Papel, UUID> {

    Optional<Papel> findByNome(String nome);
}
