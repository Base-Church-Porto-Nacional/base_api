package com.example.base_api.culto.dto;

import java.time.Instant;
import java.util.UUID;

public record CultoRelatorioResponse(
        UUID cultoId,
        Integer adultos,
        Integer criancas,
        Integer totalPresentes,
        Integer conversoes,
        Integer reconciliacoes,
        Integer aliancas,
        Integer carros,
        Integer motos,
        Integer bicicletas,
        Integer totalVeiculos,
        String observacoes,
        Instant createdAt,
        Instant updatedAt
) {
}