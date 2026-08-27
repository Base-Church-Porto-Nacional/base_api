package com.example.base_api.security;

import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Test
    void tokenInvalidoResponde401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-ruim");
        request.setRequestURI("/api/pessoas");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extrairUsername("token-ruim")).thenThrow(new io.jsonwebtoken.MalformedJwtException("bad"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void tokenComVersaoAntigaResponde401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-velho");
        request.setRequestURI("/api/pessoas");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Usuario usuario = new Usuario();
        usuario.setUsername("joao");
        usuario.setAtivo(true);
        usuario.setTokenVersion(2);

        when(jwtService.extrairUsername("token-velho")).thenReturn("joao");
        when(usuarioRepository.findByUsername("joao")).thenReturn(Optional.of(usuario));
        when(jwtService.isTokenValido("token-velho", "joao")).thenReturn(true);
        when(jwtService.extrairTokenVersion("token-velho")).thenReturn(0);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(request, response);
    }
}
