package com.example.base_api.culto.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Contagem somada dos cultos de um período. Cultos cancelados ou inativos não
 * entram; as médias consideram apenas os cultos que já têm relatório lançado.
 */
public record CultoRelatorioConsolidadoResponse(
        String competencia,
        LocalDate inicio,
        LocalDate fim,
        Integer totalCultos,
        Integer cultosComRelatorio,
        Integer cultosSemRelatorio,
        Integer adultos,
        Integer criancas,
        Integer totalPresentes,
        Integer conversoes,
        Integer reconciliacoes,
        Integer aliancas,
        Integer carros,
        Integer motos,
        Integer bicicletas,
        Integer totalVeiculos,
        Double mediaPresentes,
        Double mediaVeiculos,
        List<CultoRelatorioItemResponse> cultos
) {
}
