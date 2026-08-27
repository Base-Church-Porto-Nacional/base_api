package com.example.base_api.usuario.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;
import java.util.UUID;

public record UsuarioGestaoRequest(
        @NotEmpty(message = "Informe ao menos um papel de gestão")
        Set<String> papeis,

        UUID igrejaId
) {
}
