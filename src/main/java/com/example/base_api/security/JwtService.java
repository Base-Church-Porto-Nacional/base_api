package com.example.base_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(String username, Set<String> papeis) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + jwtProperties.expirationMs());

        return Jwts.builder()
                .subject(username)
                .claim("papeis", papeis)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(secretKey)
                .compact();
    }

    public String extrairUsername(String token) {
        return extrairClaims(token).getSubject();
    }

    public boolean isTokenValido(String token, String username) {
        String subject = extrairUsername(token);
        return subject.equals(username) && !isTokenExpirado(token);
    }

    private boolean isTokenExpirado(String token) {
        return extrairClaims(token).getExpiration().before(new Date());
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
