package com.example.base_api.voluntario.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PessoaServicoResponse(
        UUID id,
        UUID pessoaId,
        String pessoaNome,
        UUID servicoId,
        String servicoNome,
        UUID ministerioId,
        String ministerioNome,
        String cargo,
        LocalDate dataEntrada,
        Boolean ativo,
        String observacoes,
        UUID liderResponsavelId,
        Instant createdAt,
        Instant updatedAt
) {
}
