package com.example.base_api.cronograma.repository;

import com.example.base_api.cronograma.entity.CronogramaConteudo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CronogramaConteudoRepository extends JpaRepository<CronogramaConteudo, UUID> {

    Optional<CronogramaConteudo> findByEventoId(UUID eventoId);
}
