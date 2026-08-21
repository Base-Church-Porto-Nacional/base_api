package com.example.base_api.cronograma.dto;

import com.example.base_api.cronograma.entity.TipoEvento;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CronogramaEventoResponse(
        UUID id,
        String titulo,
        TipoEvento tipo,
        String descricao,
        LocalDate dataEvento,
        LocalTime horaInicio,
        LocalTime horaFim,
        String localEvento,
        Boolean ativo,
        Instant createdAt,
        Instant updatedAt
) {
}
