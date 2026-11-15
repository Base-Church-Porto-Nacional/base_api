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

    @Test
    void recusaUrlDoFrontendVaziaLocalOuSemHttps() {
        for (String url : new String[]{"", "   ", null, "http://localhost:3000", "https://localhost",
                "https://127.0.0.1:8080", "http://base.veigagustavo.com.br", "base.veigagustavo.com.br"}) {
            assertThatThrownBy(() -> StartupSecretsValidator.validarUrlFrontend(url))
                    .as("url=%s", url)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("FRONTEND_URL_BASE");
        }
    }

    @Test
    void aceitaUrlDoFrontendHttps() {
        assertThatCode(() -> StartupSecretsValidator.validarUrlFrontend("https://base.veigagustavo.com.br"))
                .doesNotThrowAnyException();
        assertThatCode(() -> StartupSecretsValidator.validarUrlFrontend(" HTTPS://base.veigagustavo.com.br/ "))
                .doesNotThrowAnyException();
    }

    @Test
    void foraDeDevFalhaComUrlDoFrontendLocal() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("app.jwt.secret", "segredo-forte-com-mais-de-trinta-e-dois-chars");
        env.setProperty("spring.datasource.password", "outra-senha");
        env.setProperty("app.frontend.url-base", "http://localhost:3000");

        assertThatThrownBy(() -> new StartupSecretsValidator(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FRONTEND_URL_BASE");
    }

    @Test
    void foraDeDevSobeComConfiguracaoDeProducao() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("app.jwt.secret", "segredo-forte-com-mais-de-trinta-e-dois-chars");
        env.setProperty("spring.datasource.password", "outra-senha");
        env.setProperty("app.frontend.url-base", "https://base.veigagustavo.com.br");

        assertThatCode(() -> new StartupSecretsValidator(env)).doesNotThrowAnyException();
    }

    @Test
    void perfilDevAceitaUrlLocal() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        env.setProperty("app.frontend.url-base", "http://localhost:3000");

        assertThatCode(() -> new StartupSecretsValidator(env)).doesNotThrowAnyException();
    }
}
