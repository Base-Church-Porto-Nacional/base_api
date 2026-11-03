package com.example.base_api.igreja.dto;

import com.example.base_api.usuario.dto.LinkAcessoResponse;

/**
 * Filial criada; {@code convite} é nulo quando o pastor já tem conta ativa. {@code participacoesAjustadas}
 * conta as participações futuras do pastor transferido que foram ajustadas na igreja de origem (0 para
 * pastor novo).
 */
public record FilialCriadaResponse(IgrejaResponse igreja, LinkAcessoResponse convite, int participacoesAjustadas) {
}
