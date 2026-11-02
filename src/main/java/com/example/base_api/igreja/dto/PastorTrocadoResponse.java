package com.example.base_api.igreja.dto;

import com.example.base_api.usuario.dto.LinkAcessoResponse;

/**
 * Pastor trocado. {@code convite} é nulo quando o novo pastor já tem conta ativa; {@code anteriorMantevePastor} é
 * true quando o pastor anterior ficou com o papel PASTOR (o novo ainda não está ativo) e precisa ser removido
 * depois, em Papéis.
 */
public record PastorTrocadoResponse(IgrejaResponse igreja, LinkAcessoResponse convite, boolean anteriorMantevePastor) {
}
