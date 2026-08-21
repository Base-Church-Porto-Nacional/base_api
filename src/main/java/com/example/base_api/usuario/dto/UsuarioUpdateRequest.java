package com.example.base_api.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UsuarioUpdateRequest(
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotNull(message = "Campo ativo é obrigatório")
        Boolean ativo,

        @NotEmpty(message = "Informe ao menos um papel")
        Set<String> papeis,

        @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres")
        String novaSenha
) {
}
