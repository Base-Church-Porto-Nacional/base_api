package com.example.base_api.security;

import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenInvalidoResponde401() throws Exception {
        MockHttpServletRequest request = requisicao("token-ruim");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.validar("token-ruim")).thenThrow(new io.jsonwebtoken.MalformedJwtException("bad"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void tokenComVersaoAntigaResponde401() throws Exception {
        MockHttpServletRequest request = requisicao("token-velho");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Usuario usuario = usuario(true, 2);

        when(jwtService.validar("token-velho")).thenReturn(new JwtService.TokenValidado("joao", 0));
        when(usuarioRepository.findByUsername("joao")).thenReturn(Optional.of(usuario));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void usuarioInativoResponde401() throws Exception {
        MockHttpServletRequest request = requisicao("token-ok");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.validar("token-ok")).thenReturn(new JwtService.TokenValidado("joao", 0));
        when(usuarioRepository.findByUsername("joao")).thenReturn(Optional.of(usuario(false, 0)));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void usuarioInexistenteResponde401() throws Exception {
        MockHttpServletRequest request = requisicao("token-ok");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.validar("token-ok")).thenReturn(new JwtService.TokenValidado("joao", 0));
        when(usuarioRepository.findByUsername("joao")).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void tokenValidoAutenticaComUmaUnicaConsultaDoUsuario() throws Exception {
        MockHttpServletRequest request = requisicao("token-ok");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Usuario usuario = usuario(true, 1);

        when(jwtService.validar("token-ok")).thenReturn(new JwtService.TokenValidado("joao", 1));
        when(usuarioRepository.findByUsername("joao")).thenReturn(Optional.of(usuario));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(usuarioRepository, times(1)).findByUsername("joao");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getName()).isEqualTo("joao");
        assertThat(auth.getPrincipal()).isInstanceOf(UsuarioAutenticado.class);
        assertThat(((UsuarioAutenticado) auth.getPrincipal()).getUsuarioId()).isEqualTo(usuario.getId());
    }

    private MockHttpServletRequest requisicao(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        request.setRequestURI("/api/pessoas");
        return request;
    }

    private Usuario usuario(boolean ativo, int tokenVersion) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setUsername("joao");
        usuario.setSenhaHash("hash");
        usuario.setAtivo(ativo);
        usuario.setTokenVersion(tokenVersion);
        return usuario;
    }
}
