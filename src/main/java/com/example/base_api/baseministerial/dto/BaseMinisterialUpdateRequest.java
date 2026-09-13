package com.example.base_api.baseministerial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record BaseMinisterialUpdateRequest(
        @NotBlank(message = "Nome da base ministerial é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String nome,

        String descricao,

        @NotNull(message = "Campo ativo é obrigatório")
        Boolean ativo,

        UUID liderPessoaId,

        UUID coordenadorPessoaId,

        String ordem
) {
}