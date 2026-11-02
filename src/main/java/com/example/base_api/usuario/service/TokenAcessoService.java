package com.example.base_api.usuario.service;

import com.example.base_api.common.exception.LinkExpiradoException;
import com.example.base_api.usuario.entity.TipoTokenAcesso;
import com.example.base_api.usuario.entity.TokenAcesso;
import com.example.base_api.usuario.repository.TokenAcessoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/** Tokens de uso único (convite e redefinição de senha). Só o hash SHA-256 é persistido. */
@Service
public class TokenAcessoService {

    static final String MENSAGEM_LINK_EXPIRADO = "Este link expirou ou já foi usado. Peça um novo convite.";
    private static final int BYTES_TOKEN = 32;

    private final SecureRandom random = new SecureRandom();
    private final TokenAcessoRepository repository;
    private final Clock clock;
    private final String urlBase;

    public TokenAcessoService(
            TokenAcessoRepository repository,
            Clock clock,
            @Value("${app.frontend.url-base}") String urlBase
    ) {
        this.repository = repository;
        this.clock = clock;
        this.urlBase = urlBase;
    }

    /** Invalida os pendentes do mesmo usuário e tipo, grava o hash do novo e devolve o token bruto. */
    @Transactional
    public String gerar(UUID usuarioId, TipoTokenAcesso tipo, UUID criadoPor) {
        repository.apagarPendentes(usuarioId, tipo);

        byte[] bytes = new byte[BYTES_TOKEN];
        random.nextBytes(bytes);
        String bruto = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        TokenAcesso token = new TokenAcesso();
        token.setUsuarioId(usuarioId);
        token.setTipo(tipo);
        token.setTokenHash(hash(bruto));
        token.setExpiraEm(clock.instant().plus(tipo.validade()));
        token.setCriadoPor(criadoPor);
        repository.save(token);
        return bruto;
    }

    @Transactional(readOnly = true)
    public TokenAcesso validar(String bruto) {
        if (bruto == null || bruto.isBlank()) {
            throw new LinkExpiradoException(MENSAGEM_LINK_EXPIRADO);
        }
        TokenAcesso token = repository.findByTokenHash(hash(bruto))
                .orElseThrow(() -> new LinkExpiradoException(MENSAGEM_LINK_EXPIRADO));
        Instant agora = clock.instant();
        if (token.getUsadoEm() != null || !token.getExpiraEm().isAfter(agora)) {
            throw new LinkExpiradoException(MENSAGEM_LINK_EXPIRADO);
        }
        return token;
    }

    /**
     * Marca como usado com update condicional; se outra requisição já usou, lança LinkExpirado.
     * Deve rodar depois de {@link #validar} na mesma transação de escrita.
     */
    @Transactional
    public void consumir(TokenAcesso token) {
        if (repository.marcarUsado(token.getId(), clock.instant()) == 0) {
            throw new LinkExpiradoException(MENSAGEM_LINK_EXPIRADO);
        }
    }

    @Transactional
    public void revogarPendentes(UUID usuarioId) {
        repository.apagarPendentes(usuarioId);
    }

    /** Há token pendente deste tipo criado dentro da janela (ex.: cooldown do esqueci a senha). */
    @Transactional(readOnly = true)
    public boolean geradoRecentemente(UUID usuarioId, TipoTokenAcesso tipo, Duration janela) {
        return repository.existsByUsuarioIdAndTipoAndUsadoEmIsNullAndCreatedAtAfter(
                usuarioId, tipo, clock.instant().minus(janela));
    }

    public String link(String bruto) {
        String base = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        return base + "/acesso/" + bruto;
    }

    private static String hash(String bruto) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bruto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
