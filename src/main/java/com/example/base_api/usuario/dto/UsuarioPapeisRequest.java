package com.example.base_api.usuario.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UsuarioPapeisRequest(
        @NotNull(message = "Informe o conjunto de papéis")
        Set<String> papeis
) {
}
