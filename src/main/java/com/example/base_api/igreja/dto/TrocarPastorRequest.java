package com.example.base_api.igreja.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TrocarPastorRequest(
        @NotNull(message = "pessoaId é obrigatório")
        UUID pessoaId
) {
}
