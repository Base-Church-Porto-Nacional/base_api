package com.example.base_api.pessoa.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PessoaHistoricoResponse(
        UUID id,
        UUID pessoaId,
        String descricao,
        String autorNome,
        String autorCargo,
        UUID autorPessoaId,
        LocalDate dataRegistro,
        Instant createdAt,
        Instant updatedAt
) {
}
