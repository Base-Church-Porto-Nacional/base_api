package com.example.base_api.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres!!";

    private final JwtService jwtService = new JwtService(new JwtProperties(SEGREDO, 60_000));

    @Test
    void validarDevolveUsernameEVersaoDoToken() {
        String token = jwtService.gerarToken("joao", null, null, null, 3);

        JwtService.TokenValidado validado = jwtService.validar(token);

        assertThat(validado.username()).isEqualTo("joao");
        assertThat(validado.tokenVersion()).isEqualTo(3);
    }

    @Test
    void validarRejeitaTokenExpirado() {
        JwtService emissorExpirado = new JwtService(new JwtProperties(SEGREDO, -1_000));
        String token = emissorExpirado.gerarToken("joao", null, null, null, 0);

        assertThatThrownBy(() -> jwtService.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void validarRejeitaTokenAssinadoComOutraChave() {
        JwtService outroEmissor = new JwtService(
                new JwtProperties("outro-segredo-tambem-com-mais-de-32-caracteres", 60_000));
        String token = outroEmissor.gerarToken("joao", null, null, null, 0);

        assertThatThrownBy(() -> jwtService.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void validarRejeitaTokenMalformado() {
        assertThatThrownBy(() -> jwtService.validar("nao-e-um-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenNaoContemPapeis() {
        String token = jwtService.gerarToken("joao", UUID.randomUUID(), UUID.randomUUID(), "MATRIZ", 1);

        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                .verifyWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token).getPayload();

        assertThat((Map<String, Object>) claims).doesNotContainKey("papeis");
        assertThat(claims).containsKeys("pessoaId", "igrejaId", "igrejaTipo", "tv");
    }
}
