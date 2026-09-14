package com.example.base_api.escala.dto;

import com.example.base_api.escala.entity.StatusEscalaVoluntario;
import jakarta.validation.constraints.NotNull;

public record EscalaVoluntarioRespostaRequest(
        @NotNull(message = "Status é obrigatório")
        StatusEscalaVoluntario status,

        String observacoes
) {
}