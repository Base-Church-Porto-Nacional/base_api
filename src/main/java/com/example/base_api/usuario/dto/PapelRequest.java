package com.example.base_api.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PapelRequest(
        @NotBlank(message = "Nome do papel é obrigatório")
        @Size(max = 50, message = "Nome deve ter no máximo 50 caracteres")
        String nome,

        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
        String descricao
) {
}
