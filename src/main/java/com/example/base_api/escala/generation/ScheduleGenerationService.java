package com.example.base_api.escala.generation;

import com.example.base_api.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Ponto de extensão para geração automática de escalas.
 * <p>
 * Não implementa o algoritmo agora. Futuramente deverá:
 * <ol>
 *   <li>Identificar serviços necessários da escala</li>
 *   <li>Encontrar voluntários qualificados (PessoaServico)</li>
 *   <li>Verificar disponibilidade semanal e indisponibilidades</li>
 *   <li>Evitar conflitos de horário</li>
 *   <li>Distribuir de forma equilibrada</li>
 *   <li>Considerar última escala realizada</li>
 *   <li>Permitir substituições e intervenção manual do líder</li>
 * </ol>
 */
@Service
public class ScheduleGenerationService {

    public void gerarSugestao(UUID escalaId) {
        throw new BusinessException(
                "Geração automática de escalas ainda não implementada. Escala: " + escalaId
        );
    }
}
