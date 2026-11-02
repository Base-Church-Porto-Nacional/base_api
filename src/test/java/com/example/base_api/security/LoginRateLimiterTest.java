package com.example.base_api.security;

import com.example.base_api.common.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class LoginRateLimiterTest {

    @Test
    void bloqueiaAposCincoFalhas() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        String user = "admin";
        String ip = "127.0.0.1";

        for (int i = 0; i < LoginRateLimiter.MAX_TENTATIVAS; i++) {
            limiter.assertPermitido(user, ip);
            limiter.registrarFalha(user, ip);
        }

        assertThatThrownBy(() -> limiter.assertPermitido(user, ip))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void sucessoLimpaTentativas() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        limiter.registrarFalha("admin", "10.0.0.1");
        limiter.limpar("admin", "10.0.0.1");

        assertThatCode(() -> limiter.assertPermitido("admin", "10.0.0.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void formatosDiferentesDoMesmoTelefoneContamJuntos() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        String ip = "127.0.0.1";
        String[] formatos = {"(63) 99999-0000", "63999990000", "+55 63 99999-0000", "5563999990000", "063999990000"};

        for (String formato : formatos) {
            limiter.assertPermitido(formato, ip);
            limiter.registrarFalha(formato, ip);
        }

        assertThatThrownBy(() -> limiter.assertPermitido("63 99999 0000", ip))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void emailEmMaiusculasContaComoOMesmo() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < LoginRateLimiter.MAX_TENTATIVAS; i++) {
            limiter.registrarFalha(i % 2 == 0 ? "Maria@Base.com" : " maria@base.com ", "10.0.0.2");
        }

        assertThatThrownBy(() -> limiter.assertPermitido("MARIA@BASE.COM", "10.0.0.2"))
                .isInstanceOf(TooManyRequestsException.class);
    }
}
