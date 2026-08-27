package com.example.base_api.igreja.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record IgrejaFilialCreateRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nome,

        @NotNull(message = "pastorPessoaId é obrigatório")
        UUID pastorPessoaId,

        @NotBlank(message = "Nome do admin é obrigatório")
        @Size(max = 150, message = "Nome do admin deve ter no máximo 150 caracteres")
        String adminNome,

        @NotBlank(message = "E-mail do admin é obrigatório")
        @Email(message = "E-mail do admin inválido")
        String adminEmail,

        @NotBlank(message = "Username do admin é obrigatório")
        @Size(min = 3, max = 100, message = "Username deve ter entre 3 e 100 caracteres")
        String adminUsername,

        @NotBlank(message = "Senha do admin é obrigatória")
        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
        String adminSenha
) {
}
