package com.example.base_api.usuario.dto;

/** Link de uso único gerado para convite ou redefinição; {@code enviadoWhatsapp} indica se o canal aceitou a mensagem. */
public record LinkAcessoResponse(String link, boolean enviadoWhatsapp) {
}
