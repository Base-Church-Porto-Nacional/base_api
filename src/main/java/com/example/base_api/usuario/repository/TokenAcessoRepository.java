package com.example.base_api.usuario.repository;

import com.example.base_api.usuario.entity.TipoTokenAcesso;
import com.example.base_api.usuario.entity.TokenAcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenAcessoRepository extends JpaRepository<TokenAcesso, UUID> {

    Optional<TokenAcesso> findByTokenHash(String tokenHash);

    boolean existsByUsuarioIdAndTipoAndUsadoEmIsNullAndCreatedAtAfter(
            UUID usuarioId, TipoTokenAcesso tipo, Instant criadoDepoisDe);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM TokenAcesso t WHERE t.usuarioId = :usuarioId AND t.tipo = :tipo AND t.usadoEm IS NULL")
    int apagarPendentes(@Param("usuarioId") UUID usuarioId, @Param("tipo") TipoTokenAcesso tipo);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM TokenAcesso t WHERE t.usuarioId = :usuarioId AND t.usadoEm IS NULL")
    int apagarPendentes(@Param("usuarioId") UUID usuarioId);

    /** Update condicional: só uma chamada concorrente vence (devolve 1); as demais recebem 0. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE TokenAcesso t SET t.usadoEm = :agora WHERE t.id = :id AND t.usadoEm IS NULL")
    int marcarUsado(@Param("id") UUID id, @Param("agora") Instant agora);
}
