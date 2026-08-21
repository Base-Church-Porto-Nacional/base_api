package com.example.base_api.disponibilidade.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record IndisponibilidadeResponse(
        UUID id,
        UUID pessoaId,
        LocalDate dataInicio,
        LocalDate dataFim,
        LocalTime horaInicio,
        LocalTime horaFim,
        String motivo,
        String observacoes,
        Instant createdAt,
        Instant updatedAt
) {
}
