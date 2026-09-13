package com.example.base_api.culto.dto;

import com.example.base_api.culto.entity.StatusCulto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CultoUpdateRequest(
        @NotNull(message = "Título é obrigatório")
        @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
        String titulo,

        @Size(max = 300, message = "Tema deve ter no máximo 300 caracteres")
        String tema,

        @NotNull(message = "Data do culto é obrigatória")
        LocalDate data,

        @NotNull(message = "Hora de início é obrigatória")
        LocalTime horaInicio,

        LocalTime horaFim,

        UUID pregadorId,

        @NotNull(message = "Status é obrigatório")
        StatusCulto status
) {
}