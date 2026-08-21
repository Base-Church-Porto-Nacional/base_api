package com.example.base_api.disponibilidade.dto;

import com.example.base_api.disponibilidade.entity.DiaSemana;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record DisponibilidadeSemanalResponse(
        UUID id,
        UUID pessoaId,
        DiaSemana diaSemana,
        LocalTime horaInicio,
        LocalTime horaFim,
        Boolean disponivel,
        String observacoes,
        Instant createdAt,
        Instant updatedAt
) {
}
