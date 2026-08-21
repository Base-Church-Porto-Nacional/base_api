package com.example.base_api.ministerio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MinisterioUpdateRequest(
        @NotBlank(message = "Nome do ministério é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String nome,

        String descricao,

        @NotNull(message = "Campo ativo é obrigatório")
        Boolean ativo
) {
}
