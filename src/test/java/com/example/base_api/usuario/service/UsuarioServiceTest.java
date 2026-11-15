package com.example.base_api.usuario.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.UsuarioAutenticado;
import com.example.base_api.usuario.dto.LoginRequest;
import com.example.base_api.usuario.dto.LoginResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.security.Capacidade;
import com.example.base_api.security.JwtService;
import com.example.base_api.usuario.dto.UsuarioPapeisRequest;
import com.example.base_api.usuario.dto.UsuarioResponse;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.StatusUsuario;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.entity.UsuarioPapel;
import com.example.base_api.usuario.repository.PapelRepository;
import com.example.base_api.usuario.repository.UsuarioPapelRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PapelRepository papelRepository;
    @Mock
    private UsuarioPapelRepository usuarioPapelRepository;
    @Mock
    private PessoaService pessoaService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private IgrejaRepository igrejaRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private TokenAcessoService tokenAcessoService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioComPapeis(UUID igrejaId, String... nomes) {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setIgrejaId(igrejaId);
        u.setStatus(StatusUsuario.ATIVO);
        for (String nome : nomes) {
            Papel papel = new Papel();
            papel.setId(UUID.randomUUID());
            papel.setNome(nome);
            if (!nome.equals("PASTOR") && !nome.equals("SECRETARIA")) {
                papel.setIgrejaId(igrejaId);
            }
            UsuarioPapel up = new UsuarioPapel();
            up.setIgrejaId(igrejaId);
            up.setPapel(papel);
            u.getPapeisIgreja().add(up);
        }
        return u;
    }

    private Papel papel(String nome, UUID igrejaId) {
        Papel p = new Papel();
        p.setId(UUID.randomUUID());
        p.setNome(nome);
        p.setIgrejaId(igrejaId);
        return p;
    }

    private void prepararSalvar() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioPapelRepository.save(any(UsuarioPapel.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void autenticarGeraTokenComIdDoUsuario() {
        UUID igrejaId = UUID.randomUUID();
        Usuario usuario = usuarioComPapeis(igrejaId);
        usuario.setPessoaId(UUID.randomUUID());
        usuario.setSenhaHash("hash");
        usuario.setTokenVersion(2);
        UsuarioAutenticado principal = UsuarioAutenticado.de(usuario);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());
        when(jwtService.gerarToken(usuario.getId(), usuario.getPessoaId(), igrejaId, null, 2)).thenReturn("jwt");

        LoginResponse response = usuarioService.autenticar(new LoginRequest("(63) 99999-0000", "senha"));

        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.usuarioId()).isEqualTo(usuario.getId());
        assertThat(response.pessoaId()).isEqualTo(usuario.getPessoaId());
    }

    @Test
    void secretariaAtribuiPapelCustomizado() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId);
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(papelRepository.findByNomeAndIgrejaId("CONTADOR", igrejaId)).thenReturn(Optional.of(papel("CONTADOR", igrejaId)));
        when(papelRepository.findByNomeAndIgrejaIdIsNull("CONTADOR")).thenReturn(Optional.empty());
        prepararSalvar();
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());

        UsuarioResponse r = usuarioService.alterarPapeis(alvo.getId(), new UsuarioPapeisRequest(Set.of(" contador ")));

        assertThat(r.papeis()).containsExactly("CONTADOR");
        verify(autorizacaoService).assertTem(igrejaId, Capacidade.PAPEIS_CUSTOM_GERIR);
        verify(autorizacaoService, never()).assertPodeAtribuirPapeisGestao(any());
        assertThat(alvo.getTokenVersion()).isNotNull();
    }

    @Test
    void secretariaNaoAtribuiSecretaria() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId);
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        doThrow(new AccessDeniedException("negado")).when(autorizacaoService).assertPodeAtribuirPapeisGestao(igrejaId);

        assertThatThrownBy(() -> usuarioService.alterarPapeis(alvo.getId(), new UsuarioPapeisRequest(Set.of("SECRETARIA"))))
                .isInstanceOf(AccessDeniedException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void pastorAtribuiSecretaria() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "CONTADOR");
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(papelRepository.findByNomeAndIgrejaId("CONTADOR", igrejaId)).thenReturn(Optional.of(papel("CONTADOR", igrejaId)));
        when(papelRepository.findByNomeAndIgrejaIdIsNull("SECRETARIA")).thenReturn(Optional.of(papel("SECRETARIA", null)));
        when(papelRepository.findByNomeAndIgrejaIdIsNull("CONTADOR")).thenReturn(Optional.empty());
        prepararSalvar();
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());

        usuarioService.alterarPapeis(alvo.getId(), new UsuarioPapeisRequest(Set.of("SECRETARIA", "CONTADOR")));

        verify(autorizacaoService).assertPodeAtribuirPapeisGestao(igrejaId);
        verify(auditService).registrar("ALTERAR_PAPEIS", "usuario",
                "usuarioId=" + alvo.getId() + " igrejaId=" + igrejaId
                        + " antes=[CONTADOR] depois=[CONTADOR, SECRETARIA]");
    }

    @Test
    void ultimoPastorNaoPodeSerRemovido() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "PASTOR");
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarioRepository.countByPapelAndIgrejaId("PASTOR", igrejaId)).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.alterarPapeis(alvo.getId(), new UsuarioPapeisRequest(Set.of("SECRETARIA"))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A igreja precisa de ao menos um pastor");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void pastorPodeSerRemovidoQuandoHaOutro() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "PASTOR");
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarioRepository.countByPapelAndIgrejaId("PASTOR", igrejaId)).thenReturn(2L);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());

        UsuarioResponse r = usuarioService.alterarPapeis(alvo.getId(), new UsuarioPapeisRequest(Set.of()));

        assertThat(r.papeis()).isEmpty();
    }

    @Test
    void papelDeOutraIgrejaEhInvalido() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId);
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(papelRepository.findByNomeAndIgrejaId("CONTADOR", igrejaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.alterarPapeis(alvo.getId(), new UsuarioPapeisRequest(Set.of("CONTADOR"))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Papel inválido: CONTADOR");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void listarBuscaNomesDePessoasEIgrejasEmLote() {
        UUID igrejaId = UUID.randomUUID();
        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setNome("Sede");
        UUID pessoaA = UUID.randomUUID();
        Usuario comPessoa = new Usuario();
        comPessoa.setId(UUID.randomUUID());
        comPessoa.setPessoaId(pessoaA);
        comPessoa.setIgrejaId(igrejaId);
        Usuario semPessoa = new Usuario();
        semPessoa.setId(UUID.randomUUID());
        semPessoa.setIgrejaId(igrejaId);

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(autorizacaoService.isGestao(igrejaId)).thenReturn(true);
        when(usuarioRepository.findByIgrejaIdIn(List.of(igrejaId))).thenReturn(List.of(comPessoa, semPessoa));
        when(pessoaService.nomesPorId(any())).thenReturn(Map.of(pessoaA, "Ana"));
        when(igrejaRepository.findAllById(any())).thenReturn(List.of(igreja));

        List<UsuarioResponse> lista = usuarioService.listar(igrejaId);

        assertThat(lista).extracting(UsuarioResponse::pessoaNome).containsExactly("Ana", null);
        assertThat(lista).extracting(UsuarioResponse::igrejaNome).containsExactly("Sede", "Sede");
        verify(pessoaService, never()).buscarEntidade(any());
        verify(igrejaRepository, never()).findById(any());
    }

    // --- conta do pastor/secretaria protegida ---

    // --- desativar / reativar ---

    private void secretariaNaoAtribuiGestao(UUID igrejaId) {
        doThrow(new AccessDeniedException("Somente o pastor"))
                .when(autorizacaoService).assertPodeAtribuirPapeisGestao(igrejaId);
    }

    @Test
    void desativarRevogaLinksEInvalidaSessao() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "CONTADOR");
        alvo.setTokenVersion(3);
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());

        UsuarioResponse r = usuarioService.desativar(alvo.getId());

        verify(autorizacaoService).assertTem(igrejaId, Capacidade.CONTAS_GERIR);
        assertThat(r.status()).isEqualTo(StatusUsuario.DESATIVADO);
        assertThat(alvo.getTokenVersion()).isEqualTo(4);
        verify(tokenAcessoService).revogarPendentes(alvo.getId());
        verify(auditService).registrar(eq("DESATIVAR_USUARIO"), eq("usuario"), contains(alvo.getId().toString()));
    }

    @Test
    void desativarJaDesativadoEhIdempotente() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "CONTADOR");
        alvo.setStatus(StatusUsuario.DESATIVADO);
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());

        UsuarioResponse r = usuarioService.desativar(alvo.getId());

        assertThat(r.status()).isEqualTo(StatusUsuario.DESATIVADO);
        verify(usuarioRepository, never()).save(any());
        verify(tokenAcessoService, never()).revogarPendentes(any());
        verify(auditService, never()).registrar(any(), any(), any());
    }

    @Test
    void secretariaNaoDesativaPastor() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "PASTOR");
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        secretariaNaoAtribuiGestao(igrejaId);

        assertThatThrownBy(() -> usuarioService.desativar(alvo.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoDesativaUltimoPastorAtivo() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "PASTOR");
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarioRepository.countByPapelAndIgrejaId("PASTOR", igrejaId)).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.desativar(alvo.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A igreja precisa de ao menos um pastor");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void reativarContaSemSenhaVoltaConvidada() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "CONTADOR");
        alvo.setStatus(StatusUsuario.DESATIVADO);
        alvo.setSenhaHash(null);
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());

        UsuarioResponse r = usuarioService.reativar(alvo.getId());

        assertThat(r.status()).isEqualTo(StatusUsuario.CONVIDADO);
        verify(auditService).registrar(eq("REATIVAR_USUARIO"), eq("usuario"), contains(alvo.getId().toString()));
    }

    @Test
    void reativarContaComSenhaVoltaAtiva() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "CONTADOR");
        alvo.setStatus(StatusUsuario.DESATIVADO);
        alvo.setSenhaHash("hash");
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());

        UsuarioResponse r = usuarioService.reativar(alvo.getId());

        assertThat(r.status()).isEqualTo(StatusUsuario.ATIVO);
    }

    @Test
    void reativarNaoGeraTokenNemRevivePendentes() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "CONTADOR");
        alvo.setStatus(StatusUsuario.DESATIVADO);
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());

        usuarioService.reativar(alvo.getId());

        verifyNoInteractions(tokenAcessoService);
    }

    @Test
    void reativarContaNaoDesativadaDevolveSemAlterar() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "CONTADOR");
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());

        UsuarioResponse r = usuarioService.reativar(alvo.getId());

        assertThat(r.status()).isEqualTo(StatusUsuario.ATIVO);
        verify(usuarioRepository, never()).save(any());
        verify(auditService, never()).registrar(any(), any(), any());
    }

    @Test
    void secretariaNaoReativaPastor() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "PASTOR");
        alvo.setStatus(StatusUsuario.DESATIVADO);
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        secretariaNaoAtribuiGestao(igrejaId);

        assertThatThrownBy(() -> usuarioService.reativar(alvo.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verify(usuarioRepository, never()).save(any());
    }
}
