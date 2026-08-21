package com.example.base_api.voluntario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record PessoaServicoUpdateRequest(
        @NotNull(message = "pessoaId é obrigatório")
        UUID pessoaId,

        @NotNull(message = "servicoId é obrigatório")
        UUID servicoId,

        @Size(max = 100, message = "Cargo deve ter no máximo 100 caracteres")
        String cargo,

        LocalDate dataEntrada,

        @NotNull(message = "Campo ativo é obrigatório")
        Boolean ativo,

        String observacoes,
        UUID liderResponsavelId
) {
}
