package com.example.base_api.escala.generation;

import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.escala.dto.GeracaoEscalaResponse;
import com.example.base_api.escala.repository.EscalaRepository;
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
public class ScheduleGenerationService {

    private final EscalaRepository escalaRepository;

    public ScheduleGenerationService(EscalaRepository escalaRepository) {
        this.escalaRepository = escalaRepository;
    }

    @Transactional(readOnly = true)
    public GeracaoEscalaResponse gerarSugestao(UUID escalaId) {
        if (!escalaRepository.existsById(escalaId)) {
            throw new ResourceNotFoundException("Escala não encontrada");
        }

        return new GeracaoEscalaResponse(
                escalaId,
                "NAO_IMPLEMENTADO",
                "Geração automática ainda não implementada. Use designação manual por enquanto.",
                List.of()
        );
    }
}
