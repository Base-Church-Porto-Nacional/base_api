package com.example.base_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(
            String username,
            Set<String> papeis,
            UUID pessoaId,
            UUID igrejaId,
            String igrejaTipo,
            int tokenVersion
    ) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + jwtProperties.expirationMs());

        var builder = Jwts.builder()
                .subject(username)
                .claim("papeis", papeis)
                .claim("tv", tokenVersion)
                .issuedAt(agora)
                .expiration(expiracao);

        if (pessoaId != null) {
            builder.claim("pessoaId", pessoaId.toString());
        }
        if (igrejaId != null) {
            builder.claim("igrejaId", igrejaId.toString());
        }
        if (igrejaTipo != null) {
            builder.claim("igrejaTipo", igrejaTipo);
        }

        return builder.signWith(secretKey).compact();
    }

    public record TokenValidado(String username, int tokenVersion) {
    }

    /**
     * Lê o token uma única vez. O parser do JJWT já rejeita assinatura inválida, token
     * malformado e token expirado lançando {@link JwtException}.
     */
    public TokenValidado validar(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        int versao = claims.get("tv") instanceof Number number ? number.intValue() : 0;
        return new TokenValidado(claims.getSubject(), versao);
    }
}
