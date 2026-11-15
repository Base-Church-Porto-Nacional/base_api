package com.example.base_api.common.exception;

/** Requisição válida, mas o estado dos dados impede a operação (HTTP 422). */
public class EntidadeNaoProcessavelException extends RuntimeException {

    public EntidadeNaoProcessavelException(String message) {
        super(message);
    }
}
