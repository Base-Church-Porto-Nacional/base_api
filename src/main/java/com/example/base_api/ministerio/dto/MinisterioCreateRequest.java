package com.example.base_api.ministerio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MinisterioCreateRequest(
        @NotBlank(message = "Nome do ministério é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String nome,

        String descricao
) {
}
