package com.example.base_api.escala.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EscalaServicoUpsertRequest(
        @NotNull(message = "servicoId é obrigatório")
        UUID servicoId,

        @NotNull(message = "quantidadeNecessaria é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser no mínimo 1")
        Integer quantidadeNecessaria
) {
}
