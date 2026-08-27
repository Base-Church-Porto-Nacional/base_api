package com.example.base_api.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class StartupSecretsValidatorTest {

    @Test
    void recusaJwtDefaultForaDeDev() {
        assertThatThrownBy(() -> StartupSecretsValidator.validarJwt(
                "change-me-use-a-long-secure-secret-key-at-least-256-bits"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void recusaSenhaDeBancoPadrao() {
        assertThatThrownBy(() -> StartupSecretsValidator.validarSenhaBanco("igreja"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD");
    }

    @Test
    void aceitaSegredosFortes() {
        assertThatCode(() -> StartupSecretsValidator.validarJwt("segredo-forte-com-mais-de-trinta-e-dois-chars"))
                .doesNotThrowAnyException();
        assertThatCode(() -> StartupSecretsValidator.validarSenhaBanco("outra-senha"))
                .doesNotThrowAnyException();
    }

    @Test
    void ignoraValidacaoNoPerfilDev() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        env.setProperty("app.jwt.secret", "change-me");
        env.setProperty("spring.datasource.password", "igreja");

        assertThatCode(() -> new StartupSecretsValidator(env)).doesNotThrowAnyException();
    }
}
