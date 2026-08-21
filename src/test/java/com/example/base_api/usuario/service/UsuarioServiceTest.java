package com.example.base_api.usuario.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.JwtService;
import com.example.base_api.usuario.dto.UsuarioCreateRequest;
import com.example.base_api.usuario.dto.UsuarioResponse;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.PapelRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PapelRepository papelRepository;
    @Mock
    private PessoaService pessoaService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void deveCriarUsuarioComPapeis() {
        Papel membro = new Papel();
        membro.setId(UUID.randomUUID());
        membro.setNome("MEMBRO");

        when(usuarioRepository.existsByUsername("joao")).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("joao@email.com")).thenReturn(false);
        when(papelRepository.findByNome("MEMBRO")).thenReturn(Optional.of(membro));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            u.setUpdatedAt(Instant.now());
            return u;
        });

        UsuarioResponse response = usuarioService.criar(new UsuarioCreateRequest(
                null, "joao", "joao@email.com", "senha123", Set.of("MEMBRO")
        ));

        assertThat(response.username()).isEqualTo("joao");
        assertThat(response.papeis()).contains("MEMBRO");
    }

    @Test
    void naoDeveCriarUsuarioComUsernameDuplicado() {
        when(usuarioRepository.existsByUsername("joao")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criar(new UsuarioCreateRequest(
                null, "joao", "joao@email.com", "senha123", Set.of("MEMBRO")
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username");
    }

    @Test
    void naoDeveCriarUsuarioComPapelInvalido() {
        when(usuarioRepository.existsByUsername("joao")).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("joao@email.com")).thenReturn(false);
        when(papelRepository.findByNome("XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.criar(new UsuarioCreateRequest(
                null, "joao", "joao@email.com", "senha123", Set.of("XYZ")
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Papel inválido");
    }
}
