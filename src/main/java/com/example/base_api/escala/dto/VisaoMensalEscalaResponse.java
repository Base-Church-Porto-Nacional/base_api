package com.example.base_api.escala.dto;

import com.example.base_api.escala.entity.StatusEscala;
import com.example.base_api.escala.entity.StatusEscalaVoluntario;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record VisaoMensalEscalaResponse(
        String competencia,
        UUID igrejaId,
        UUID ministerioId,
        String ministerioNome,
        UUID escalaId,
        String escalaTitulo,
        StatusEscala escalaStatus,
        List<ItemVisao> itens
) {
    public record ItemVisao(
            String origem,
            UUID id,
            String titulo,
            LocalDate data,
            LocalTime horaInicio,
            LocalTime horaFim,
            String localEvento,
            List<ServicoVisao> servicos
    ) {
    }

    public record ServicoVisao(
            UUID servicoId,
            String servicoNome,
            Integer quantidadeNecessaria,
            List<DesignadoVisao> designados
    ) {
    }

    public record DesignadoVisao(
            UUID designacaoId,
            UUID pessoaId,
            String pessoaNome,
            StatusEscalaVoluntario status,
            Boolean confirmadoPeloVoluntario,
            String observacoes
    ) {
    }
}