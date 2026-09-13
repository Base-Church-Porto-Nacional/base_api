package com.example.base_api.baseministerial.dto;

import java.time.Instant;
import java.util.UUID;

public record BaseMinisterialResponse(
        UUID id,
        String nome,
        String descricao,
        Boolean ativo,
        UUID igrejaId,
        UUID liderPessoaId,
        String liderNome,
        UUID coordenadorPessoaId,
        String coordenadorNome,
        String ordem,
        Instant createdAt,
        Instant updatedAt
) {
}