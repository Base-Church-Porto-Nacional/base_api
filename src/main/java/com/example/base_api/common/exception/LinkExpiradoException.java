package com.example.base_api.common.exception;

/** Link de convite/redefinição desconhecido, expirado ou já usado (HTTP 410). */
public class LinkExpiradoException extends RuntimeException {

    public LinkExpiradoException(String message) {
        super(message);
    }
}
