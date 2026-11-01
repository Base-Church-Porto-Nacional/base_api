package com.example.base_api.escala.dto;

import com.example.base_api.common.entity.DiaSemana;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CandidatoDisponibilidadeResponse(
        UUID servicoId,
        String servicoNome,
        UUID pessoaId,
        String pessoaNome,
        List<String> servicosQueServe,
        List<DiaDisponivel> diasDisponiveis,
        AnaliseDia analiseDia
) {
    public record DiaDisponivel(
            DiaSemana diaSemana,
            LocalTime horaInicio,
            LocalTime horaFim,
            Boolean disponivel
    ) {
    }

    public record AnaliseDia(
            LocalDate data,
            String tituloEvento,
            boolean disponivel,
            List<String> motivosIndisponibilidade,
            List<Conflito> conflitos
    ) {
    }

    public record Conflito(
            UUID escalaId,
            String escalaTitulo,
            String ministerioNome,
            String servicoNome,
            LocalDate data,
            LocalTime horario
    ) {
    }
}