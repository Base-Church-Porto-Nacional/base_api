package com.example.base_api.escala.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EscalaVoluntarioCreateRequest(
        @NotNull(message = "pessoaId é obrigatório")
        UUID pessoaId,

        @NotNull(message = "servicoId é obrigatório")
        UUID servicoId,

        String observacoes
) {
}
