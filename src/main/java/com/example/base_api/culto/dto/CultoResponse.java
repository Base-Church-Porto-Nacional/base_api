package com.example.base_api.culto.dto;

import com.example.base_api.culto.entity.StatusCulto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CultoResponse(
        UUID id,
        UUID igrejaId,
        UUID categoriaCultoId,
        String categoriaCultoTitulo,
        String titulo,
        String tema,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        UUID pregadorId,
        String pregadorNome,
        StatusCulto status,
        String competencia,
        UUID escalaId,
        Boolean ativo,
        Boolean janelaConfiguracaoAberta,
        Boolean temRelatorio,
        Instant createdAt,
        Instant updatedAt
) {
}