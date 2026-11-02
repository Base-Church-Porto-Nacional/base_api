package com.example.base_api.usuario.dto;

import jakarta.validation.constraints.NotBlank;

public record EsqueciSenhaRequest(
        @NotBlank(message = "E-mail ou telefone é obrigatório")
        String identificador
) {
}
