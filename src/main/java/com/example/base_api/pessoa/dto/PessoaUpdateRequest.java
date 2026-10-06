package com.example.base_api.pessoa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PessoaUpdateRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nome,

        @Size(max = 150, message = "Nome social deve ter no máximo 150 caracteres")
        String nomeSocial,

        @Email(message = "E-mail inválido")
        @Size(max = 255, message = "E-mail deve ter no máximo 255 caracteres")
        String email,

        @Size(max = 30, message = "Telefone deve ter no máximo 30 caracteres")
        String telefone,

        @Size(max = 30, message = "WhatsApp deve ter no máximo 30 caracteres")
        String whatsapp,

        LocalDate dataNascimento,

        @NotNull(message = "Campo ativo é obrigatório")
        Boolean ativo,

        String observacoes
) {
}
