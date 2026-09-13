package com.example.base_api.smallgroup.dto;

import com.example.base_api.smallgroup.entity.DiaSemana;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SmallGroupCreateRequest(
        @NotBlank(message = "Nome do pequeno grupo é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String nome,

        @NotNull(message = "igrejaId é obrigatório")
        UUID igrejaId,

        @NotNull(message = "Dia da semana é obrigatório")
        DiaSemana diaSemana,

        @Size(max = 10, message = "Horário deve ter no máximo 10 caracteres")
        String horario,

        @NotNull(message = "O pequeno grupo deve ter um líder")
        UUID liderPessoaId,

        UUID ministerioId,

        Boolean ativo,

        @Min(value = 0, message = "Nota deve estar entre 0 e 5")
        @Max(value = 5, message = "Nota deve estar entre 0 e 5")
        Integer nota
) {
}