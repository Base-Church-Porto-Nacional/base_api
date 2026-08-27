package com.example.base_api.igreja.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record IgrejaUpdateRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nome,

        @NotNull(message = "pastorPessoaId é obrigatório")
        UUID pastorPessoaId,

        @NotNull(message = "Campo ativo é obrigatório")
        Boolean ativo
) {
}
