package com.example.base_api.usuario.dto;

import com.example.base_api.usuario.entity.TipoTokenAcesso;

/** Dados mínimos de um link de acesso válido, para a tela de definir senha. */
public record TokenInfoResponse(TipoTokenAcesso tipo, String primeiroNome) {
}
