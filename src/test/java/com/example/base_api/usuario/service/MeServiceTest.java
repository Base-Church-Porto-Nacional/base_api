package com.example.base_api.usuario.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.igreja.service.IgrejaContextoService;
import com.example.base_api.igreja.service.IgrejaService;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.Capacidade;
import com.example.base_api.security.PermissaoService;
import com.example.base_api.security.Permissoes;
import com.example.base_api.usuario.dto.MeResponse;
import com.example.base_api.usuario.dto.MeUpdateRequest;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PessoaRepository pessoaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private IgrejaContextoService igrejaContextoService;
    @Mock private IgrejaService igrejaService;
    @Mock private PermissaoService permissaoService;

    @InjectMocks
    private MeService meService;

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private final Authentication auth = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(USUARIO_ID);
        u.setEmail("joao@igreja.com");
        u.setSenhaHash("hash-antigo");
        u.setTokenVersion(0);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(u));
        lenient().when(usuarioRepository.findByEmailIgnoreCase("joao@igreja.com")).thenReturn(Optional.of(u));
        return u;
    }

    private MeUpdateRequest request(String novaSenha, String senhaAtual) {
        return new MeUpdateRequest("João", null, "joao@igreja.com", null, null, null, novaSenha, senhaAtual);
    }

    @Test
    void trocaDeSenhaSemSenhaAtualEhRecusada() {
        Usuario u = usuario();

        assertThatThrownBy(() -> meService.atualizarPerfil(auth, request("novaSenha123", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Senha atual incorreta");

        assertThat(u.getSenhaHash()).isEqualTo("hash-antigo");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void trocaDeSenhaComSenhaAtualErradaEhRecusada() {
        Usuario u = usuario();
        when(passwordEncoder.matches("errada", "hash-antigo")).thenReturn(false);

        assertThatThrownBy(() -> meService.atualizarPerfil(auth, request("novaSenha123", "errada")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Senha atual incorreta");

        assertThat(u.getSenhaHash()).isEqualTo("hash-antigo");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void meDevolvePermissoesDoPastor() {
        usuario();
        UUID igrejaId = UUID.randomUUID();
        when(permissaoService.doUsuarioAtual()).thenReturn(new Permissoes(
                igrejaId, Set.of("PASTOR"), EnumSet.allOf(Capacidade.class),
                Set.of(), Set.of(), Set.of(), false));

        MeResponse r = meService.obterPerfil(auth);

        List<String> esperado = Arrays.stream(Capacidade.values()).map(Enum::name).sorted().toList();
        assertThat(esperado).hasSize(14);
        assertThat(r.permissoes().capacidades()).containsExactlyElementsOf(esperado);
        assertThat(r.permissoes().ehVoluntario()).isFalse();
    }

    @Test
    void meDeLiderSemPapelDevolveMinisteriosLiderados() {
        usuario();
        UUID ministerio = UUID.randomUUID();
        UUID grupo = UUID.randomUUID();
        when(permissaoService.doUsuarioAtual()).thenReturn(new Permissoes(
                UUID.randomUUID(), Set.of(), Set.of(),
                Set.of(ministerio), Set.of(), Set.of(grupo), true));

        MeResponse r = meService.obterPerfil(auth);

        assertThat(r.permissoes().capacidades()).isEmpty();
        assertThat(r.permissoes().ministeriosLiderados()).containsExactly(ministerio);
        assertThat(r.permissoes().gruposLiderados()).containsExactly(grupo);
        assertThat(r.permissoes().basesLideradas()).isEmpty();
        assertThat(r.permissoes().ehVoluntario()).isTrue();
        assertThat(r.papeis()).isEmpty();
    }
}
