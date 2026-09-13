package com.example.base_api.ministerio.dto;

import com.example.base_api.ministerio.entity.MinisterioTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MinisterioCreateRequest(
        @NotBlank(message = "Nome do ministério é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String nome,

        String descricao,

        Boolean ativo,

        @NotNull(message = "igrejaId é obrigatório")
        UUID igrejaId,

        MinisterioTipo tipo,

        UUID liderPessoaId,

        UUID coordenadorPessoaId,

        UUID baseMinisterialId
) {
}