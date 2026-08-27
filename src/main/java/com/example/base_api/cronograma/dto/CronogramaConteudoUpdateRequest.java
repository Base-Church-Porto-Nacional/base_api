package com.example.base_api.cronograma.dto;

import java.util.UUID;

public record CronogramaConteudoUpdateRequest(
        String textoPregacao,
        String louvor,
        UUID pregadorPessoaId
) {
}
