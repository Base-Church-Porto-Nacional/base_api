package com.example.base_api.jornada.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JornadaEtapaCatalogoRequest(
        @NotBlank(message = "Código é obrigatório")
        @Size(max = 80, message = "Código deve ter no máximo 80 caracteres")
        String codigo,

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nome,

        String descricao,
        Integer ordem,

        @NotNull(message = "Campo ativo é obrigatório")
        Boolean ativo
) {
}
