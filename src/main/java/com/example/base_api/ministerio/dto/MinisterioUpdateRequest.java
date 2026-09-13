package com.example.base_api.ministerio.dto;

import com.example.base_api.ministerio.entity.MinisterioTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MinisterioUpdateRequest(
        @NotBlank(message = "Nome do ministério é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String nome,

        String descricao,

        @NotNull(message = "Campo ativo é obrigatório")
        Boolean ativo,

        MinisterioTipo tipo,

        UUID liderPessoaId,

        UUID coordenadorPessoaId,

        String anotacoes,

        String ordem,

        UUID baseMinisterialId
) {
}