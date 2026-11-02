package com.example.base_api.security;

import com.example.base_api.usuario.entity.Usuario;
import org.springframework.security.core.userdetails.User;

import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Principal do Spring Security que carrega o id do {@link Usuario}, para que o contexto
 * resolva o usuário por id (servido pelo cache da sessão JPA dentro da transação)
 * em vez de repetir a consulta pelo identificador de login.
 */
public class UsuarioAutenticado extends User {

    private final UUID usuarioId;

    private UsuarioAutenticado(Usuario usuario) {
        super(
                // Nome do principal = id do usuário (o mesmo do sub do JWT).
                usuario.getId().toString(),
                // Conta CONVIDADO ainda não tem senha; User não aceita senha nula.
                usuario.getSenhaHash() == null ? "" : usuario.getSenhaHash(),
                usuario.isAtivo(),
                true,
                true,
                true,
                // Sem authorities: permissões são calculadas no servidor (PermissaoService).
                List.of()
        );
        this.usuarioId = usuario.getId();
    }

    public static UsuarioAutenticado de(Usuario usuario) {
        return new UsuarioAutenticado(usuario);
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    /** Id do usuário autenticado: do principal ou, na falta dele, do nome (= id, como o sub do JWT). */
    public static Optional<UUID> idDe(Authentication authentication) {
        if (authentication == null) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof UsuarioAutenticado principal) {
            return Optional.ofNullable(principal.getUsuarioId());
        }
        try {
            return authentication.getName() == null
                    ? Optional.empty()
                    : Optional.of(UUID.fromString(authentication.getName()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
