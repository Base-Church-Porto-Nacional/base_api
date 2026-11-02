package com.example.base_api.usuario.entity;

import java.time.Duration;

public enum TipoTokenAcesso {
    CONVITE(Duration.ofDays(7)),
    REDEFINICAO(Duration.ofHours(1));

    private final Duration validade;

    TipoTokenAcesso(Duration validade) {
        this.validade = validade;
    }

    public Duration validade() {
        return validade;
    }
}
