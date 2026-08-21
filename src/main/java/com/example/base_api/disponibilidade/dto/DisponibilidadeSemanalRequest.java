package com.example.base_api.disponibilidade.dto;

import com.example.base_api.disponibilidade.entity.DiaSemana;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record DisponibilidadeSemanalRequest(
        @NotNull(message = "pessoaId é obrigatório")
        UUID pessoaId,

        @NotNull(message = "diaSemana é obrigatório")
        DiaSemana diaSemana,

        @NotNull(message = "horaInicio é obrigatória")
        LocalTime horaInicio,

        @NotNull(message = "horaFim é obrigatória")
        LocalTime horaFim,

        @NotNull(message = "disponivel é obrigatório")
        Boolean disponivel,

        String observacoes
) {
}
