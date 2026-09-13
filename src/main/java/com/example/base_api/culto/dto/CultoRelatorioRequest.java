package com.example.base_api.culto.dto;

import jakarta.validation.constraints.Min;

public record CultoRelatorioRequest(
        @Min(value = 0, message = "Adultos não pode ser negativo")
        Integer adultos,

        @Min(value = 0, message = "Crianças não pode ser negativo")
        Integer criancas,

        @Min(value = 0, message = "Conversões não pode ser negativo")
        Integer conversoes,

        @Min(value = 0, message = "Reconciliações não pode ser negativo")
        Integer reconciliacoes,

        @Min(value = 0, message = "Alianças não pode ser negativo")
        Integer aliancas,

        @Min(value = 0, message = "Carros não pode ser negativo")
        Integer carros,

        @Min(value = 0, message = "Motos não pode ser negativo")
        Integer motos,

        @Min(value = 0, message = "Bicicletas não pode ser negativo")
        Integer bicicletas,

        String observacoes
) {
}