package com.example.base_api.usuario.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "E-mail ou telefone é obrigatório")
        String identificador,

        @NotBlank(message = "Senha é obrigatória")
        String senha
) {
}
