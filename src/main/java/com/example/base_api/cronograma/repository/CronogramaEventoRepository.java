package com.example.base_api.cronograma.repository;

import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.entity.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CronogramaEventoRepository extends JpaRepository<CronogramaEvento, UUID> {

    List<CronogramaEvento> findByDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
            LocalDate inicio,
            LocalDate fim
    );

    List<CronogramaEvento> findByTipoAndAtivoTrueOrderByDataEventoAsc(TipoEvento tipo);
}
