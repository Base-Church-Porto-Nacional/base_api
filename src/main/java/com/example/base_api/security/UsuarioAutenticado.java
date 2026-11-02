package com.example.base_api.security;

import com.example.base_api.usuario.entity.Usuario;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.UUID;

/**
 * Principal do Spring Security que carrega o id do {@link Usuario}, para que o contexto
 * resolva o usuário por id (servido pelo cache da sessão JPA dentro da transação)
 * em vez de repetir a consulta por username.
 */
public class UsuarioAutenticado extends User {

    private final UUID usuarioId;

    private UsuarioAutenticado(Usuario usuario) {
        super(
                usuario.getUsername(),
                usuario.getSenhaHash(),
                Boolean.TRUE.equals(usuario.getAtivo()),
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
}
