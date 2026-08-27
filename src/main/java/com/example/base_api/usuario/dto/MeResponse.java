package com.example.base_api.usuario.dto;

import com.example.base_api.igreja.dto.IgrejaResponse;
import com.example.base_api.igreja.entity.IgrejaTipo;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record MeResponse(
        UUID id,
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
        String observacoes,
        UUID igrejaId,
        IgrejaTipo igrejaTipo,
        String igrejaNome,
        List<IgrejaResponse> igrejasVisiveis
) {
}
