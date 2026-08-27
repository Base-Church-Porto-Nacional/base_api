package com.example.base_api.servico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServicoCreateRequest(
        @NotBlank(message = "Nome do serviço é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String nome,

        String descricao,

        Boolean ficcao
) {
}
