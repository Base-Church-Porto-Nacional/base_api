package com.example.base_api.usuario.dto;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record MeResponse(
        UUID usuarioId,
        String username,
        String email,
        Boolean ativo,
        Set<String> papeis,
        UUID pessoaId,
        String nome,
        String nomeSocial,
        String telefone,
        LocalDate dataNascimento,
        String observacoes
) {
}
