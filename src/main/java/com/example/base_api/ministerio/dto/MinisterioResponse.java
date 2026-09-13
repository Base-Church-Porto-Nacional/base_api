package com.example.base_api.ministerio.dto;

import com.example.base_api.ministerio.entity.MinisterioTipo;
import com.example.base_api.servico.dto.ServicoResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MinisterioResponse(
        UUID id,
        String nome,
        String descricao,
        Boolean ativo,
        UUID igrejaId,
        MinisterioTipo tipo,
        UUID liderPessoaId,
        String liderNome,
        UUID coordenadorPessoaId,
        String coordenadorNome,
        UUID baseMinisterialId,
        String baseMinisterialNome,
        String anotacoes,
        String ordem,
        Integer totalVoluntarios,
        Integer totalServicos,
        List<ServicoResponse> servicos,
        Instant createdAt,
        Instant updatedAt
) {
}