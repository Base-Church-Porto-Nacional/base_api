package com.example.base_api.escala.dto;

import com.example.base_api.escala.entity.StatusEscala;
import jakarta.validation.constraints.NotNull;

public record EscalaStatusRequest(
        @NotNull(message = "Status é obrigatório")
        StatusEscala status
) {
}