package com.example.base_api.usuario.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.pessoa.entity.Pessoa;
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
import jakarta.validation.Validation;
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

    private Pessoa pessoaDoUsuario(Usuario u) {
        Pessoa p = new Pessoa();
        p.setId(UUID.randomUUID());
        p.setNome("João");
        p.setEmail("joao@igreja.com");
        u.setPessoaId(p.getId());
        when(pessoaRepository.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    private void stubResposta() {
        lenient().when(permissaoService.doUsuarioAtual()).thenReturn(new Permissoes(
                UUID.randomUUID(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), false));
    }

    private MeUpdateRequest comContato(String email, String telefone) {
        return new MeUpdateRequest("João", null, email, telefone, null, null, null, null);
    }

    @Test
    void meTrocaEmailAtualizaLogin() {
        Usuario u = usuario();
        Pessoa p = pessoaDoUsuario(u);
        stubResposta();
        when(usuarioRepository.findByEmailIgnoreCase("novo@igreja.com")).thenReturn(Optional.empty());

        meService.atualizarPerfil(auth, comContato("Novo@Igreja.com", null));

        assertThat(u.getEmail()).isEqualTo("novo@igreja.com");
        assertThat(p.getEmail()).isEqualTo("novo@igreja.com");
    }

    @Test
    void meEmailDeOutraContaLanca409() {
        Usuario u = usuario();
        Usuario outro = new Usuario();
        outro.setId(UUID.randomUUID());
        when(usuarioRepository.findByEmailIgnoreCase("novo@igreja.com")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> meService.atualizarPerfil(auth, comContato("novo@igreja.com", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("E-mail já usado por outra conta");
        assertThat(u.getEmail()).isEqualTo("joao@igreja.com");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void meTrocaTelefoneAtualizaLoginNormalizado() {
        Usuario u = usuario();
        pessoaDoUsuario(u);
        stubResposta();
        when(usuarioRepository.findByTelefone("5563999990000")).thenReturn(Optional.empty());

        meService.atualizarPerfil(auth, comContato("joao@igreja.com", "(63) 99999-0000"));

        assertThat(u.getTelefone()).isEqualTo("5563999990000");
    }

    @Test
    void meTelefoneDeOutraContaLanca409() {
        Usuario u = usuario();
        pessoaDoUsuario(u);
        Usuario outro = new Usuario();
        outro.setId(UUID.randomUUID());
        when(usuarioRepository.findByTelefone("5563999990000")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> meService.atualizarPerfil(auth, comContato("joao@igreja.com", "63999990000")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Telefone já usado por outra conta");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void meNaoPodeFicarSemEmailNemTelefone() {
        Usuario u = usuario();
        u.setEmail(null);
        u.setTelefone("5563999990000");
        Pessoa p = pessoaDoUsuario(u);
        p.setTelefone("63999990000");

        assertThatThrownBy(() -> meService.atualizarPerfil(auth, comContato(null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Informe e-mail ou telefone para entrar no app");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void meSenhaCurtaFalha() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violacoes = factory.getValidator().validate(request("curta12", "atual"));
            assertThat(violacoes).hasSize(1);
            assertThat(factory.getValidator().validate(request("longa1234", "atual"))).isEmpty();
        }
    }
}
