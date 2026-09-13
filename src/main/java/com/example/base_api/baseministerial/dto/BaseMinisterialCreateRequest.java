package com.example.base_api.baseministerial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record BaseMinisterialCreateRequest(
        @NotBlank(message = "Nome da base ministerial é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String nome,

        String descricao,

        Boolean ativo,

        @NotNull(message = "igrejaId é obrigatório")
        UUID igrejaId,

        UUID liderPessoaId,

        UUID coordenadorPessoaId
) {
}