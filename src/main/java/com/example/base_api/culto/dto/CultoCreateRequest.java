package com.example.base_api.culto.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CultoCreateRequest(
        @NotNull(message = "Categoria de culto é obrigatória")
        UUID categoriaCultoId,

        UUID igrejaId,

        @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
        String titulo,

        @Size(max = 300, message = "Tema deve ter no máximo 300 caracteres")
        String tema,

        @NotNull(message = "Data do culto é obrigatória")
        LocalDate data,

        LocalTime horaInicio,

        LocalTime horaFim,

        UUID pregadorId
) {
}