package com.example.base_api.usuario.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username é obrigatório")
        String username,

        @NotBlank(message = "Senha é obrigatória")
        String senha
) {
}
