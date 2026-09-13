package com.example.base_api.culto.dto;

import java.util.UUID;

public record CultoConteudoUpdateRequest(
        String textoPregacao,
        String louvor,
        String tema,
        UUID pregadorPessoaId
) {
}