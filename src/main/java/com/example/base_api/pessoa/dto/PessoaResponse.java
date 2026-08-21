package com.example.base_api.pessoa.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PessoaResponse(
        UUID id,
        String nome,
        String nomeSocial,
        String email,
        String telefone,
        LocalDate dataNascimento,
        LocalDate dataCadastro,
        Boolean ativo,
        String observacoes,
        Instant createdAt,
        Instant updatedAt
) {
}
