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
}
