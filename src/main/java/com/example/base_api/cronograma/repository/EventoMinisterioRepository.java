package com.example.base_api.cronograma.repository;

import com.example.base_api.cronograma.entity.EventoMinisterio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventoMinisterioRepository extends JpaRepository<EventoMinisterio, UUID> {

    List<EventoMinisterio> findByEventoId(UUID eventoId);

    Optional<EventoMinisterio> findByEventoIdAndMinisterioId(UUID eventoId, UUID ministerioId);

}