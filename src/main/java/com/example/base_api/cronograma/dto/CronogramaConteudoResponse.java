package com.example.base_api.cronograma.dto;

import java.util.UUID;

public record CronogramaConteudoResponse(
        UUID eventoId,
        String textoPregacao,
        String louvor,
        UUID pregadorPessoaId,
        String pregadorNome
) {
}
