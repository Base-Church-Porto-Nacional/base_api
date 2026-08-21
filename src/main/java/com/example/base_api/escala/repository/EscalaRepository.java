package com.example.base_api.escala.repository;

import com.example.base_api.escala.entity.Escala;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EscalaRepository extends JpaRepository<Escala, UUID> {

    List<Escala> findByDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(LocalDate inicio, LocalDate fim);
}
