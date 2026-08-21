package com.example.base_api.dashboard.dto;

public record DashboardResponse(
        long totalPessoas,
        long totalPessoasAtivas,
        long totalMinisterios,
        long totalServicos,
        long totalVoluntariosAtivos,
        long totalEscalas,
        long totalUsuarios,
        long totalLideres,
        long totalEventosCronograma,
        long totalPausasAtivas
) {
}
