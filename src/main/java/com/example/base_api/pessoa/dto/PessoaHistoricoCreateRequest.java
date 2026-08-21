package com.example.base_api.pessoa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record PessoaHistoricoCreateRequest(
        @NotBlank(message = "Descrição é obrigatória")
        String descricao,

        @NotBlank(message = "Nome de quem registrou o comentário é obrigatório")
        @Size(max = 150, message = "Nome do autor deve ter no máximo 150 caracteres")
        String autorNome,

        @Size(max = 100, message = "Cargo deve ter no máximo 100 caracteres")
        String autorCargo,

        UUID autorPessoaId,

        LocalDate dataRegistro
) {
}
