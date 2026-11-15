package com.example.base_api.security;

import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Recebe o identificador digitado no login (e-mail ou telefone). Conta CONVIDADO ou DESATIVADO
     * é tratada como inexistente, para responder 401 "Credenciais inválidas" igual à senha errada.
     */
    @Override
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByIdentificador(identificador)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return UsuarioAutenticado.de(usuario);
    }
}
