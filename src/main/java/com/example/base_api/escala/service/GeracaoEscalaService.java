package com.example.base_api.escala.service;

import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.escala.dto.GeracaoEscalaResponse;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.security.AutorizacaoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Ponto de extensão para geração automática de escalas.
 * <p>
 * Não implementa o algoritmo completo ainda. Futuramente deverá:
 * <ol>
 *   <li>Identificar serviços necessários da escala</li>
 *   <li>Encontrar voluntários qualificados (PessoaServico)</li>
 *   <li>Verificar disponibilidade, pausas e indisponibilidades</li>
 *   <li>Evitar conflitos de horário</li>
 *   <li>Distribuir de forma equilibrada</li>
 *   <li>Considerar última escala realizada</li>
 *   <li>Permitir substituições e intervenção manual do líder</li>
 * </ol>
 */
@Service
public class GeracaoEscalaService {

    private final EscalaRepository escalaRepository;
    private final AutorizacaoService autorizacaoService;

    public GeracaoEscalaService(EscalaRepository escalaRepository, AutorizacaoService autorizacaoService) {
        this.escalaRepository = escalaRepository;
        this.autorizacaoService = autorizacaoService;
    }

    @Transactional(readOnly = true)
    public GeracaoEscalaResponse gerarSugestao(UUID escalaId) {
        Escala escala = escalaRepository.findById(escalaId)
                .orElseThrow(() -> new ResourceNotFoundException("Escala não encontrada"));
        autorizacaoService.assertPodeMutarEscala(
                escala.getTipo(),
                escala.getIgrejaId(),
                escala.getCompetencia(),
                escala.getMinisterioId()
        );

        return new GeracaoEscalaResponse(
                escalaId,
                "NAO_IMPLEMENTADO",
                "Geração automática ainda não implementada. Use designação manual por enquanto.",
                List.of()
        );
    }
}
