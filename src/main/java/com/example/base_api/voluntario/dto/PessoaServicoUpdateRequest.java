package com.example.base_api.voluntario.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record PessoaServicoUpdateRequest(
        LocalDate dataEntrada,

        @NotNull(message = "Campo ativo é obrigatório")
        Boolean ativo,

        String observacoes,
        UUID liderResponsavelId
) {
}
