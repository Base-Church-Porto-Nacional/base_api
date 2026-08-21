package com.example.base_api.escala.dto;

import com.example.base_api.escala.entity.StatusEscalaVoluntario;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EscalaVoluntarioUpdateRequest(
        @NotNull(message = "pessoaId é obrigatório")
        UUID pessoaId,

        @NotNull(message = "servicoId é obrigatório")
        UUID servicoId,

        @NotNull(message = "Status é obrigatório")
        StatusEscalaVoluntario status,

        String observacoes
) {
}
