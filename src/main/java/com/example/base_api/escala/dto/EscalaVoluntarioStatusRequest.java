package com.example.base_api.escala.dto;

import com.example.base_api.escala.entity.StatusEscalaVoluntario;
import jakarta.validation.constraints.NotNull;

public record EscalaVoluntarioStatusRequest(
        @NotNull(message = "Status é obrigatório")
        StatusEscalaVoluntario status,

        String observacoes,

        Boolean confirmadoPeloVoluntario
) {
}
