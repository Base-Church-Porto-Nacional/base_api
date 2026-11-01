package com.example.base_api.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres!!";

    private final JwtService jwtService = new JwtService(new JwtProperties(SEGREDO, 60_000));

    @Test
    void validarDevolveUsernameEVersaoDoToken() {
        String token = jwtService.gerarToken("joao", Set.of("MEMBRO"), null, null, null, 3);

        JwtService.TokenValidado validado = jwtService.validar(token);

        assertThat(validado.username()).isEqualTo("joao");
        assertThat(validado.tokenVersion()).isEqualTo(3);
    }

    @Test
    void validarRejeitaTokenExpirado() {
        JwtService emissorExpirado = new JwtService(new JwtProperties(SEGREDO, -1_000));
        String token = emissorExpirado.gerarToken("joao", Set.of(), null, null, null, 0);

        assertThatThrownBy(() -> jwtService.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void validarRejeitaTokenAssinadoComOutraChave() {
        JwtService outroEmissor = new JwtService(
                new JwtProperties("outro-segredo-tambem-com-mais-de-32-caracteres", 60_000));
        String token = outroEmissor.gerarToken("joao", Set.of(), null, null, null, 0);

        assertThatThrownBy(() -> jwtService.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void validarRejeitaTokenMalformado() {
        assertThatThrownBy(() -> jwtService.validar("nao-e-um-jwt")).isInstanceOf(JwtException.class);
    }
}
