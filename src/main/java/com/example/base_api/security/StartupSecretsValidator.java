package com.example.base_api.security;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class StartupSecretsValidator {

    static final String JWT_DEFAULT_PREFIX = "change-me";
    static final String DB_PASSWORD_DEFAULT = "igreja";
    static final int JWT_SECRET_MIN_LENGTH = 32;

    public StartupSecretsValidator(Environment environment) {
        Set<String> profiles = Set.copyOf(Arrays.asList(environment.getActiveProfiles()));
        if (profiles.contains("dev") || profiles.contains("test")) {
            return;
        }
        validarJwt(environment.getProperty("app.jwt.secret", ""));
        validarSenhaBanco(environment.getProperty("spring.datasource.password", ""));
    }

    static void validarJwt(String secret) {
        if (secret == null || secret.isBlank() || secret.startsWith(JWT_DEFAULT_PREFIX)
                || secret.length() < JWT_SECRET_MIN_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET obrigatório fora dos perfis dev/test: use um segredo aleatório com pelo menos 32 caracteres"
            );
        }
    }

    static void validarSenhaBanco(String password) {
        if (password == null || password.isBlank() || DB_PASSWORD_DEFAULT.equals(password)) {
            throw new IllegalStateException(
                    "DB_PASSWORD não pode ser o valor padrão fora dos perfis dev/test"
            );
        }
    }
}
