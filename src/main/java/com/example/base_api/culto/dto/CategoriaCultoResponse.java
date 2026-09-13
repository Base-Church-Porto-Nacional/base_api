package com.example.base_api.culto.dto;

import com.example.base_api.culto.entity.DiaSemana;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record CategoriaCultoResponse(
        UUID id,
        UUID igrejaId,
        String titulo,
        String descricao,
        DiaSemana diaSemana,
        LocalTime horaInicio,
        Integer duracaoMinutos,
        UUID pregadorPadraoId,
        String pregadorPadraoNome,
        Boolean ativo,
        Instant createdAt,
        Instant updatedAt
) {
}