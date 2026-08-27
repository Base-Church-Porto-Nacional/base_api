package com.example.base_api.cronograma.repository;

import com.example.base_api.cronograma.entity.CronogramaEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CronogramaEventoRepository extends JpaRepository<CronogramaEvento, UUID> {

    List<CronogramaEvento> findByIgrejaIdInOrderByDataEventoAscHoraInicioAsc(List<UUID> igrejaIds);

    List<CronogramaEvento> findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
            List<UUID> igrejaIds,
            LocalDate inicio,
            LocalDate fim
    );

    long countByIgrejaIdIn(List<UUID> igrejaIds);
}
