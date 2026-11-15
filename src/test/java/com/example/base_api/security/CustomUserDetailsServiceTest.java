package com.example.base_api.security;

import com.example.base_api.usuario.entity.StatusUsuario;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void carregaPorEmail() {
        Usuario usuario = usuario(StatusUsuario.ATIVO);
        when(usuarioRepository.findByIdentificador(anyString())).thenCallRealMethod();
        when(usuarioRepository.findByEmailIgnoreCase("maria@base.com")).thenReturn(Optional.of(usuario));

        UserDetails detalhes = service.loadUserByUsername(" Maria@Base.COM ");

        assertThat(detalhes).isInstanceOf(UsuarioAutenticado.class);
        assertThat(((UsuarioAutenticado) detalhes).getUsuarioId()).isEqualTo(usuario.getId());
        assertThat(detalhes.getUsername()).isEqualTo(usuario.getId().toString());
        assertThat(detalhes.isEnabled()).isTrue();
    }

    @Test
    void carregaPorTelefoneFormatado() {
        Usuario usuario = usuario(StatusUsuario.ATIVO);
        when(usuarioRepository.findByIdentificador(anyString())).thenCallRealMethod();
        when(usuarioRepository.findByTelefone("5563999990000")).thenReturn(Optional.of(usuario));

        assertThat(((UsuarioAutenticado) service.loadUserByUsername("(63) 99999-0000")).getUsuarioId())
                .isEqualTo(usuario.getId());
        assertThat(((UsuarioAutenticado) service.loadUserByUsername("+55 63 99999-0000")).getUsuarioId())
                .isEqualTo(usuario.getId());
        assertThat(((UsuarioAutenticado) service.loadUserByUsername("63999990000")).getUsuarioId())
                .isEqualTo(usuario.getId());
    }

    @Test
    void contaConvidadaNaoAutentica() {
        when(usuarioRepository.findByIdentificador("maria@base.com"))
                .thenReturn(Optional.of(usuario(StatusUsuario.CONVIDADO)));

        assertThatThrownBy(() -> service.loadUserByUsername("maria@base.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void contaDesativadaNaoAutentica() {
        when(usuarioRepository.findByIdentificador("maria@base.com"))
                .thenReturn(Optional.of(usuario(StatusUsuario.DESATIVADO)));

        assertThatThrownBy(() -> service.loadUserByUsername("maria@base.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void identificadorDesconhecidoNaoAutentica() {
        when(usuarioRepository.findByIdentificador("ninguem@base.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ninguem@base.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private Usuario usuario(StatusUsuario status) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setPessoaId(UUID.randomUUID());
        usuario.setEmail("maria@base.com");
        usuario.setTelefone("5563999990000");
        usuario.setSenhaHash(status == StatusUsuario.CONVIDADO ? null : "hash");
        usuario.setStatus(status);
        return usuario;
    }
}
