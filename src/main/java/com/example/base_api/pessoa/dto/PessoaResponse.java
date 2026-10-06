package com.example.base_api.pessoa.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record PessoaResponse(
        UUID id,
        String nome,
        String nomeSocial,
        String email,
        String telefone,
        String whatsapp,
        LocalDate dataNascimento,
        LocalDate dataCadastro,
        Boolean ativo,
        String observacoes,
        UUID igrejaId,
        UUID usuarioId,
        Set<String> papeis,
        Instant createdAt,
        Instant updatedAt
) {
}
