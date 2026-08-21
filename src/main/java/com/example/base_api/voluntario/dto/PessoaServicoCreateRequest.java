package com.example.base_api.voluntario.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record PessoaServicoCreateRequest(
        @NotNull(message = "pessoaId é obrigatório")
        UUID pessoaId,

        @NotNull(message = "servicoId é obrigatório")
        UUID servicoId,

        LocalDate dataEntrada,
        String observacoes,
        UUID liderResponsavelId
) {
}
