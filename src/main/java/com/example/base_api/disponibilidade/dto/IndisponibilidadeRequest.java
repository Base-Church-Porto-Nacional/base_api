package com.example.base_api.disponibilidade.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record IndisponibilidadeRequest(
        @NotNull(message = "pessoaId é obrigatório")
        UUID pessoaId,

        @NotNull(message = "dataInicio é obrigatória")
        LocalDate dataInicio,

        @NotNull(message = "dataFim é obrigatória")
        LocalDate dataFim,

        LocalTime horaInicio,
        LocalTime horaFim,
        String motivo,
        String observacoes
) {
}
