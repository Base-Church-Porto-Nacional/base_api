package com.example.base_api.igreja.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IgrejaFilialCreateRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nome,

        @NotNull(message = "Pastor é obrigatório")
        @Valid
        PastorFilialRequest pastor
) {
}
