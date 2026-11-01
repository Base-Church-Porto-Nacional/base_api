package com.example.base_api.smallgroup.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SmallGroupParticipanteRequest(
        UUID pessoaId,

        @NotNull(message = "Nome do participante é obrigatório")
        String pessoaNome
) {
}