package com.example.base_api.culto.dto;

import java.util.UUID;

public record CultoConteudoResponse(
        UUID cultoId,
        String textoPregacao,
        String louvor,
        String tema,
        UUID pregadorId,
        String pregadorNome
) {
}