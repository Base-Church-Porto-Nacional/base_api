package com.example.base_api.usuario.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.security.Capacidade;
import com.example.base_api.security.JwtService;
import com.example.base_api.usuario.dto.UsuarioCreateRequest;
import com.example.base_api.usuario.dto.UsuarioPapeisRequest;
import com.example.base_api.usuario.dto.UsuarioResponse;
import com.example.base_api.usuario.dto.UsuarioUpdateRequest;
import com.example.base_api.usuario.entity.Papel;
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

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioComPapeis(UUID igrejaId, String... nomes) {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setUsername("alvo");
        u.setIgrejaId(igrejaId);
        u.setAtivo(true);
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
    void usuarioCriadoSemPapeis() {
        UUID igrejaId = UUID.randomUUID();
        when(usuarioRepository.existsByUsername("joao")).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("joao@email.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());

        UsuarioResponse response = usuarioService.criar(new UsuarioCreateRequest(
                null, "joao", "joao@email.com", "senha123", igrejaId
        ));

        assertThat(response.username()).isEqualTo("joao");
        assertThat(response.papeis()).isEmpty();
        verify(usuarioPapelRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarUsuarioComUsernameDuplicado() {
        UUID igrejaId = UUID.randomUUID();
        when(usuarioRepository.existsByUsername("joao")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criar(new UsuarioCreateRequest(
                null, "joao", "joao@email.com", "senha123", igrejaId
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username");
    }

    @Test
    void membroNaoCriaUsuario() {
        UUID igrejaId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Somente pastor ou secretaria desta igreja"))
                .when(autorizacaoService).assertPodeCriarUsuario(igrejaId);

        assertThatThrownBy(() -> usuarioService.criar(new UsuarioCreateRequest(
                null, "joao", "joao@email.com", "senha123", igrejaId
        ))).isInstanceOf(AccessDeniedException.class);
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
        comPessoa.setUsername("ana");
        comPessoa.setPessoaId(pessoaA);
        comPessoa.setIgrejaId(igrejaId);
        Usuario semPessoa = new Usuario();
        semPessoa.setId(UUID.randomUUID());
        semPessoa.setUsername("sistema");
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

    private void secretariaNaoAtribuiGestao(UUID igrejaId) {
        doThrow(new AccessDeniedException("Somente o pastor"))
                .when(autorizacaoService).assertPodeAtribuirPapeisGestao(igrejaId);
    }

    @Test
    void secretariaNaoRedefineSenhaDoPastor() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "PASTOR");
        alvo.setEmail("pastor@email.com");
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        secretariaNaoAtribuiGestao(igrejaId);

        assertThatThrownBy(() -> usuarioService.atualizar(alvo.getId(),
                new UsuarioUpdateRequest("pastor@email.com", true, "novasenha")))
                .isInstanceOf(AccessDeniedException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void secretariaNaoDesativaPastor() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "PASTOR");
        alvo.setEmail("pastor@email.com");
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        secretariaNaoAtribuiGestao(igrejaId);

        assertThatThrownBy(() -> usuarioService.atualizar(alvo.getId(),
                new UsuarioUpdateRequest("pastor@email.com", false, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void secretariaNaoExcluiOutraSecretaria() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "SECRETARIA");
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        secretariaNaoAtribuiGestao(igrejaId);

        assertThatThrownBy(() -> usuarioService.excluir(alvo.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verify(usuarioRepository, never()).excluirDireto(any());
    }

    @Test
    void secretariaAtualizaEmailDoPastorSemTocarSenhaOuAtivo() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "PASTOR");
        alvo.setEmail("pastor@email.com");
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());

        usuarioService.atualizar(alvo.getId(), new UsuarioUpdateRequest("novo@email.com", true, null));

        verify(autorizacaoService, never()).assertPodeAtribuirPapeisGestao(any());
        assertThat(alvo.getEmail()).isEqualTo("novo@email.com");
    }

    @Test
    void secretariaRedefineSenhaDeUsuarioComum() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "CONTADOR");
        alvo.setEmail("c@email.com");
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("novasenha")).thenReturn("hash2");

        usuarioService.atualizar(alvo.getId(), new UsuarioUpdateRequest("c@email.com", true, "novasenha"));

        verify(autorizacaoService, never()).assertPodeAtribuirPapeisGestao(any());
        assertThat(alvo.getSenhaHash()).isEqualTo("hash2");
    }

    @Test
    void pastorNaoDesativaUltimoPastorAtivo() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "PASTOR");
        alvo.setEmail("pastor@email.com");
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarioRepository.countByPapelAndIgrejaId("PASTOR", igrejaId)).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.atualizar(alvo.getId(),
                new UsuarioUpdateRequest("pastor@email.com", false, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A igreja precisa de ao menos um pastor");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void pastorNaoExcluiUltimoPastorAtivo() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "PASTOR");
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
        when(usuarioRepository.countByPapelAndIgrejaId("PASTOR", igrejaId)).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.excluir(alvo.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A igreja precisa de ao menos um pastor");
        verify(usuarioRepository, never()).excluirDireto(any());
    }

    @Test
    void pastorExcluiPastorInativoMesmoComUmUnicoAtivo() {
        UUID igrejaId = UUID.randomUUID();
        Usuario alvo = usuarioComPapeis(igrejaId, "PASTOR");
        alvo.setAtivo(false);
        when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));

        usuarioService.excluir(alvo.getId());

        verify(usuarioRepository).excluirDireto(alvo.getId());
    }
}
