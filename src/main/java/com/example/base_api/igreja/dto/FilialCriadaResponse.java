package com.example.base_api.igreja.dto;

import com.example.base_api.usuario.dto.LinkAcessoResponse;

/** Filial criada; {@code convite} é nulo quando o pastor já tem conta ativa. */
public record FilialCriadaResponse(IgrejaResponse igreja, LinkAcessoResponse convite) {
}
