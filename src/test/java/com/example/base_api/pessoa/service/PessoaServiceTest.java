package com.example.base_api.pessoa.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.jornada.entity.JornadaMembro;
import com.example.base_api.jornada.service.JornadaService;
import com.example.base_api.pessoa.dto.PessoaCreateRequest;
import com.example.base_api.pessoa.dto.PessoaResponse;
import com.example.base_api.pessoa.dto.PessoaUpdateRequest;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.security.Capacidade;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import com.example.base_api.pessoa.dto.PessoaResumoResponse;
import org.mockito.ArgumentCaptor;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.StatusUsuario;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.entity.UsuarioPapel;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PessoaServiceTest {

    @Mock
    private PessoaRepository pessoaRepository;

    @Mock
    private JornadaService jornadaService;

    @Mock
    private AutorizacaoService autorizacaoService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PessoaServicoRepository pessoaServicoRepository;

    @Mock
    private com.example.base_api.usuario.service.LiderService liderService;

    @Mock
    private com.example.base_api.audit.service.AuditService auditService;

    @InjectMocks
    private PessoaService pessoaService;

    private Pessoa pessoa;
    private UUID igrejaId;

    @BeforeEach
    void setUp() {
        igrejaId = UUID.randomUUID();
        pessoa = new Pessoa();
        pessoa.setId(UUID.randomUUID());
        pessoa.setNome("João da Silva");
        pessoa.setEmail("joao@email.com");
        pessoa.setAtivo(true);
        pessoa.setIgrejaId(igrejaId);
        pessoa.setDataCadastro(LocalDate.now());
        pessoa.setCreatedAt(Instant.now());
        pessoa.setUpdatedAt(Instant.now());
    }

    @Test
    void deveCriarPessoaEIniciarJornada() {
        PessoaCreateRequest request = new PessoaCreateRequest(
                "Maria Souza", null, "maria@email.com", "11999999999", null, null, null, igrejaId, null
        );

        when(pessoaRepository.findByEmailIgnoreCase("maria@email.com")).thenReturn(Optional.empty());
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> {
            Pessoa salva = invocation.getArgument(0);
            salva.setId(UUID.randomUUID());
            salva.setCreatedAt(Instant.now());
            salva.setUpdatedAt(Instant.now());
            return salva;
        });
        when(usuarioRepository.findByPessoaId(any())).thenReturn(Optional.empty());

        PessoaResponse response = pessoaService.criar(request);

        assertThat(response.nome()).isEqualTo("Maria Souza");
        assertThat(response.email()).isEqualTo("maria@email.com");
        assertThat(response.igrejaId()).isEqualTo(igrejaId);
        verify(jornadaService).criarJornadaInicial(response.id());
        verify(autorizacaoService).assertPodeCadastrarPessoa(igrejaId, null);
    }

    @Test
    void naoDeveCriarPessoaComEmailDuplicado() {
        when(pessoaRepository.findByEmailIgnoreCase("joao@email.com")).thenReturn(Optional.of(pessoa));

        PessoaCreateRequest request = new PessoaCreateRequest(
                "Outro", null, "joao@email.com", null, null, null, null, igrejaId, null
        );

        assertThatThrownBy(() -> pessoaService.criar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    void deveBuscarPessoaPorId() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());

        PessoaResponse response = pessoaService.buscarPorId(pessoa.getId());

        assertThat(response.id()).isEqualTo(pessoa.getId());
        assertThat(response.nome()).isEqualTo("João da Silva");
        assertThat(response.igrejaId()).isEqualTo(igrejaId);
        verify(autorizacaoService).assertPodeVerPessoa(pessoa);
    }

    @Test
    void deveLancarExcecaoQuandoPessoaNaoExiste() {
        UUID id = UUID.randomUUID();
        when(pessoaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pessoaService.buscarPorId(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveAtualizarPessoa() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        when(pessoaRepository.existsByEmailIgnoreCaseAndIdNot("joao@email.com", pessoa.getId())).thenReturn(false);
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());
        when(autorizacaoService.tem(igrejaId, Capacidade.DADOS_PASTORAIS_EDITAR)).thenReturn(true);

        PessoaUpdateRequest request = new PessoaUpdateRequest(
                "João Atualizado", null, "joao@email.com", "11888888888", "11977776666", null, true, "obs"
        );

        PessoaResponse response = pessoaService.atualizar(pessoa.getId(), request);

        assertThat(response.nome()).isEqualTo("João Atualizado");
        assertThat(response.telefone()).isEqualTo("11888888888");
        assertThat(response.whatsapp()).isEqualTo("11977776666");
        assertThat(response.observacoes()).isEqualTo("obs");
        verify(autorizacaoService).assertPodeEditarContato(pessoa);
    }

    @Test
    void deveListarPessoasDaIgrejaVisivel() {
        when(autorizacaoService.idsParaListagem(null)).thenReturn(List.of(igrejaId));
        when(pessoaRepository.findByIgrejaIdInOrderByNomeAsc(List.of(igrejaId))).thenReturn(List.of(pessoa));
        when(autorizacaoService.filtrarVisiveisNaListagem(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<PessoaResponse> lista = pessoaService.listar(null);

        assertThat(lista).hasSize(1);
    }

    @Test
    void filialNaoListaPessoasDeOutraIgreja() {
        UUID outraIgreja = UUID.randomUUID();
        doThrow(new AccessDeniedException("Sem acesso a esta igreja"))
                .when(autorizacaoService).idsParaListagem(outraIgreja);

        assertThatThrownBy(() -> pessoaService.listar(outraIgreja))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deveIncluirPapeisDaPessoaNaResposta() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.of(usuarioComPapeis()));

        PessoaResponse response = pessoaService.buscarPorId(pessoa.getId());

        assertThat(response.papeis()).contains("VOLUNTARIO", "LIDER");
    }

    @Test
    void naoDeveIncluirPapeisDeIgrejaDiferente() {
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.of(usuarioComPapeis()));

        PessoaResponse response = pessoaService.toResponse(pessoa);

        assertThat(response.papeis()).doesNotContain("ADMIN");
    }

    @Test
    void listarBuscaUsuariosDasPessoasNumaUnicaConsulta() {
        Pessoa outra = new Pessoa();
        outra.setId(UUID.randomUUID());
        outra.setNome("Ana");
        outra.setIgrejaId(igrejaId);
        Usuario usuario = usuarioComPapeis();
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(pessoaRepository.findByIgrejaIdInOrderByNomeAsc(List.of(igrejaId))).thenReturn(List.of(outra, pessoa));
        when(autorizacaoService.filtrarVisiveisNaListagem(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByPessoaIdIn(List.of(outra.getId(), pessoa.getId()))).thenReturn(List.of(usuario));

        List<PessoaResponse> lista = pessoaService.listar(igrejaId);

        assertThat(lista).extracting(PessoaResponse::usuarioId).containsExactly(null, usuario.getId());
        assertThat(lista.get(1).papeis()).contains("VOLUNTARIO", "LIDER");
        verify(usuarioRepository, never()).findByPessoaId(any());
    }

    // --- líder cadastra e edita o próprio pessoal ---

    private PessoaCreateRequest requestComServico(UUID servicoId) {
        return new PessoaCreateRequest("Maria Souza", null, null, "11999999999", null, null, null, igrejaId, servicoId);
    }

    private void salvarGerandoId() {
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> {
            Pessoa salva = invocation.getArgument(0);
            if (salva.getId() == null) {
                salva.setId(UUID.randomUUID());
            }
            salva.setCreatedAt(Instant.now());
            salva.setUpdatedAt(Instant.now());
            return salva;
        });
    }

    @Test
    void liderCadastraPessoaNoServicoDoSeuMinisterio() {
        UUID servicoId = UUID.randomUUID();
        salvarGerandoId();
        when(usuarioRepository.findByPessoaId(any())).thenReturn(Optional.empty());

        PessoaResponse response = pessoaService.criar(requestComServico(servicoId));

        verify(autorizacaoService).assertPodeCadastrarPessoa(igrejaId, servicoId);
        ArgumentCaptor<PessoaServico> captor = ArgumentCaptor.forClass(PessoaServico.class);
        verify(pessoaServicoRepository).save(captor.capture());
        assertThat(captor.getValue().getPessoaId()).isEqualTo(response.id());
        assertThat(captor.getValue().getServicoId()).isEqualTo(servicoId);
        assertThat(captor.getValue().getAtivo()).isTrue();
    }

    @Test
    void liderNaoCadastraComObservacoes() {
        PessoaCreateRequest request = new PessoaCreateRequest(
                "Maria", null, null, null, null, null, "nota", igrejaId, UUID.randomUUID());

        assertThatThrownBy(() -> pessoaService.criar(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Somente pastor ou secretaria alteram dados pastorais");
        verify(pessoaRepository, never()).save(any());
        verify(pessoaServicoRepository, never()).save(any());
    }

    @Test
    void liderSemServicoNaoCadastra() {
        doThrow(new AccessDeniedException("Sem permissão para esta ação"))
                .when(autorizacaoService).assertPodeCadastrarPessoa(igrejaId, null);

        assertThatThrownBy(() -> pessoaService.criar(requestComServico(null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(pessoaRepository, never()).save(any());
        verify(pessoaServicoRepository, never()).save(any());
    }

    @Test
    void liderEmServicoDeOutroMinisterioNaoCadastra() {
        UUID servicoId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Sem permissão para esta ação"))
                .when(autorizacaoService).assertPodeCadastrarPessoa(igrejaId, servicoId);

        assertThatThrownBy(() -> pessoaService.criar(requestComServico(servicoId)))
                .isInstanceOf(AccessDeniedException.class);
        verify(pessoaRepository, never()).save(any());
    }

    @Test
    void liderEditaContatoDoSeuVoluntario() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());
        pessoa.setObservacoes("nota pastoral");

        PessoaUpdateRequest request = new PessoaUpdateRequest(
                "João Novo", null, "joao@email.com", "11888888888", null, null, true, "nota pastoral");

        PessoaResponse response = pessoaService.atualizar(pessoa.getId(), request);

        assertThat(response.nome()).isEqualTo("João Novo");
        assertThat(response.telefone()).isEqualTo("11888888888");
        assertThat(response.observacoes()).isEqualTo("nota pastoral");
        assertThat(response.ativo()).isTrue();
        verify(autorizacaoService).assertPodeEditarContato(pessoa);
    }

    @Test
    void liderNaoAlteraObservacoes() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        pessoa.setObservacoes("original");

        PessoaUpdateRequest request = new PessoaUpdateRequest(
                "João", null, "joao@email.com", null, null, null, true, "alterada");

        assertThatThrownBy(() -> pessoaService.atualizar(pessoa.getId(), request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Somente pastor ou secretaria alteram dados pastorais");
        verify(pessoaRepository, never()).save(any());
    }

    @Test
    void liderNaoDesativaPessoa() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));

        PessoaUpdateRequest request = new PessoaUpdateRequest(
                "João", null, "joao@email.com", null, null, null, false, null);

        assertThatThrownBy(() -> pessoaService.atualizar(pessoa.getId(), request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Somente pastor ou secretaria alteram dados pastorais");
        verify(pessoaRepository, never()).save(any());
    }

    @Test
    void liderNaoEditaPessoaComVinculoInativo() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        doThrow(new AccessDeniedException("Sem permissão para editar esta pessoa"))
                .when(autorizacaoService).assertPodeEditarContato(pessoa);

        PessoaUpdateRequest request = new PessoaUpdateRequest(
                "João", null, "joao@email.com", null, null, null, true, null);

        assertThatThrownBy(() -> pessoaService.atualizar(pessoa.getId(), request))
                .isInstanceOf(AccessDeniedException.class);
        verify(pessoaRepository, never()).save(any());
    }

    @Test
    void liderNaoExcluiPessoa() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        doThrow(new AccessDeniedException("Sem permissão para esta ação"))
                .when(autorizacaoService).assertTem(igrejaId, Capacidade.PESSOAS_GERIR);

        assertThatThrownBy(() -> pessoaService.excluir(pessoa.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verify(pessoaRepository, never()).delete(any());
    }

    @Test
    void buscaPorNomeExigeDoisCaracteres() {
        assertThatThrownBy(() -> pessoaService.buscarPorNome("a"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> pessoaService.buscarPorNome(" a "))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> pessoaService.buscarPorNome(null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void buscaPorNomeLimitaVinteResultados() {
        List<Pessoa> muitas = new java.util.ArrayList<>();
        for (int i = 0; i < 25; i++) {
            Pessoa p = new Pessoa();
            p.setId(UUID.randomUUID());
            p.setNome("Ana " + i);
            p.setIgrejaId(igrejaId);
            muitas.add(p);
        }
        when(autorizacaoService.igrejasParaBuscaDePessoas()).thenReturn(List.of(igrejaId));
        when(pessoaRepository.findTop20ByIgrejaIdInAndAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(List.of(igrejaId), "Ana"))
                .thenReturn(muitas);

        List<PessoaResumoResponse> resultado = pessoaService.buscarPorNome(" Ana ");

        assertThat(resultado).hasSize(20);
        assertThat(resultado.get(0).nome()).isEqualTo("Ana 0");
        verify(autorizacaoService).assertPodeBuscarPessoas(List.of(igrejaId));
    }

    @Test
    void membroSemLiderancaNaoBusca() {
        when(autorizacaoService.igrejasParaBuscaDePessoas()).thenReturn(List.of(igrejaId));
        doThrow(new AccessDeniedException("Sem permissão para buscar pessoas"))
                .when(autorizacaoService).assertPodeBuscarPessoas(List.of(igrejaId));

        assertThatThrownBy(() -> pessoaService.buscarPorNome("Ana"))
                .isInstanceOf(AccessDeniedException.class);
        verify(pessoaRepository, never())
                .findTop20ByIgrejaIdInAndAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(any(), any());
        verify(autorizacaoService, never()).idsParaListagem(any());
    }

    private Usuario usuarioComPapeis() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setPessoaId(pessoa.getId());
        usuario.setIgrejaId(igrejaId);
        usuario.setEmail("pessoa@email.com");
        usuario.setSenhaHash("hash");

        Papel voluntario = new Papel();
        voluntario.setId(UUID.randomUUID());
        voluntario.setNome("VOLUNTARIO");

        Papel lider = new Papel();
        lider.setId(UUID.randomUUID());
        lider.setNome("LIDER");

        Papel admin = new Papel();
        admin.setId(UUID.randomUUID());
        admin.setNome("ADMIN");

        UUID outraIgreja = UUID.randomUUID();

        UsuarioPapel vp = new UsuarioPapel();
        vp.setUsuarioId(usuario.getId());
        vp.setPapelId(voluntario.getId());
        vp.setIgrejaId(igrejaId);
        vp.setPapel(voluntario);

        UsuarioPapel lp = new UsuarioPapel();
        lp.setUsuarioId(usuario.getId());
        lp.setPapelId(lider.getId());
        lp.setIgrejaId(igrejaId);
        lp.setPapel(lider);

        UsuarioPapel ap = new UsuarioPapel();
        ap.setUsuarioId(usuario.getId());
        ap.setPapelId(admin.getId());
        ap.setIgrejaId(outraIgreja);
        ap.setPapel(admin);

        usuario.getPapeisIgreja().add(vp);
        usuario.getPapeisIgreja().add(lp);
        usuario.getPapeisIgreja().add(ap);
        return usuario;
    }

    @Test
    void listarIncluiEtapasPrincipaisDaJornadaNumaUnicaConsulta() {
        JornadaMembro jornada = new JornadaMembro();
        jornada.setPessoaId(pessoa.getId());
        jornada.setAceitouJesus(true);
        jornada.setBatizado(true);
        jornada.setConcluiuTrilhaBase(false);
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(pessoaRepository.findByIgrejaIdInOrderByNomeAsc(List.of(igrejaId))).thenReturn(List.of(pessoa));
        when(autorizacaoService.filtrarVisiveisNaListagem(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jornadaService.buscarPorPessoas(List.of(pessoa.getId()))).thenReturn(Map.of(pessoa.getId(), jornada));

        PessoaResponse response = pessoaService.listar(igrejaId).getFirst();

        assertThat(response.aceitouJesus()).isTrue();
        assertThat(response.batizado()).isTrue();
        assertThat(response.concluiuTrilhaBase()).isFalse();
    }

    @Test
    void respostaSemJornadaDeixaEtapasNulas() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());

        PessoaResponse response = pessoaService.buscarPorId(pessoa.getId());

        assertThat(response.batizado()).isNull();
    }

    // --- funções derivadas (pós-V18) ---

    private Usuario usuarioCom(UUID pessoaId, boolean ativo, String... papeisSistema) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setPessoaId(pessoaId);
        usuario.setIgrejaId(igrejaId);
        usuario.setSenhaHash("hash");
        usuario.setStatus(ativo ? StatusUsuario.ATIVO : StatusUsuario.DESATIVADO);
        for (String nome : papeisSistema) {
            Papel papel = new Papel();
            papel.setId(UUID.randomUUID());
            papel.setNome(nome);
            UsuarioPapel up = new UsuarioPapel();
            up.setUsuarioId(usuario.getId());
            up.setPapelId(papel.getId());
            up.setIgrejaId(igrejaId);
            up.setPapel(papel);
            usuario.getPapeisIgreja().add(up);
        }
        return usuario;
    }

    private Pessoa novaPessoa(String nome) {
        Pessoa p = new Pessoa();
        p.setId(UUID.randomUUID());
        p.setNome(nome);
        p.setIgrejaId(igrejaId);
        p.setAtivo(true);
        return p;
    }

    @Test
    void listarDerivaFuncoesEmLote() {
        Pessoa pastor = novaPessoa("Ana");
        Pessoa lider = novaPessoa("Bruno");
        Pessoa visitante = novaPessoa("Carla");
        Usuario usuarioPastor = usuarioCom(pastor.getId(), true, "PASTOR");
        Papel custom = new Papel();
        custom.setId(UUID.randomUUID());
        custom.setNome("CONTADOR");
        custom.setIgrejaId(igrejaId);
        UsuarioPapel cp = new UsuarioPapel();
        cp.setIgrejaId(igrejaId);
        cp.setPapel(custom);
        usuarioPastor.getPapeisIgreja().add(cp);
        List<UUID> ids = List.of(pastor.getId(), lider.getId(), visitante.getId());
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(pessoaRepository.findByIgrejaIdInOrderByNomeAsc(List.of(igrejaId)))
                .thenReturn(List.of(pastor, lider, visitante));
        when(autorizacaoService.filtrarVisiveisNaListagem(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByPessoaIdIn(ids)).thenReturn(List.of(usuarioPastor));
        when(liderService.pessoaIdsLideres(List.of(igrejaId))).thenReturn(java.util.Set.of(lider.getId()));
        when(pessoaServicoRepository.pessoaIdsComVinculoAtivo(ids)).thenReturn(List.of(lider.getId()));

        List<PessoaResponse> lista = pessoaService.listar(igrejaId);

        assertThat(lista.get(0).funcoes()).containsExactlyInAnyOrder("PASTOR", "MEMBRO");
        assertThat(lista.get(1).funcoes()).containsExactlyInAnyOrder("LIDER", "VOLUNTARIO");
        assertThat(lista.get(2).funcoes()).isEmpty();
        verify(liderService).pessoaIdsLideres(List.of(igrejaId));
        verify(pessoaServicoRepository).pessoaIdsComVinculoAtivo(ids);
        verify(usuarioRepository, never()).findByPessoaId(any());
    }

    @Test
    void pessoaResponseTrazAcessoStatusEmLote() {
        Pessoa convidada = novaPessoa("Ana");
        Pessoa semConta = novaPessoa("Bruno");
        Usuario usuario = usuarioCom(convidada.getId(), false, "MEMBRO");
        usuario.setStatus(com.example.base_api.usuario.entity.StatusUsuario.CONVIDADO);
        List<UUID> ids = List.of(convidada.getId(), semConta.getId());
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(pessoaRepository.findByIgrejaIdInOrderByNomeAsc(List.of(igrejaId)))
                .thenReturn(List.of(convidada, semConta));
        when(autorizacaoService.filtrarVisiveisNaListagem(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByPessoaIdIn(ids)).thenReturn(List.of(usuario));

        List<PessoaResponse> lista = pessoaService.listar(igrejaId);

        assertThat(lista.get(0).acessoStatus()).isEqualTo(com.example.base_api.usuario.entity.StatusUsuario.CONVIDADO);
        assertThat(lista.get(1).acessoStatus()).isNull();
        verify(usuarioRepository, never()).findByPessoaId(any());
    }

    @Test
    void usuarioInativoNaoContaComoMembro() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        when(usuarioRepository.findByPessoaId(pessoa.getId()))
                .thenReturn(Optional.of(usuarioCom(pessoa.getId(), false, "SECRETARIA")));

        PessoaResponse response = pessoaService.buscarPorId(pessoa.getId());

        assertThat(response.funcoes()).containsExactly("SECRETARIA");
    }

    // --- PUT parcial ---

    @Test
    void atualizacaoParcialNaoApagaCamposOmitidos() {
        pessoa.setNomeSocial("Jo");
        pessoa.setDataNascimento(LocalDate.of(1990, 1, 2));
        pessoa.setObservacoes("nota");
        pessoa.setTelefone("111");
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());
        
        PessoaUpdateRequest request = new PessoaUpdateRequest(
                null, null, null, "222", null, null, null, null);

        PessoaResponse response = pessoaService.atualizar(pessoa.getId(), request);

        assertThat(response.nome()).isEqualTo("João da Silva");
        assertThat(response.nomeSocial()).isEqualTo("Jo");
        assertThat(response.email()).isEqualTo("joao@email.com");
        assertThat(response.dataNascimento()).isEqualTo(LocalDate.of(1990, 1, 2));
        assertThat(response.observacoes()).isEqualTo("nota");
        assertThat(response.ativo()).isTrue();
        assertThat(response.telefone()).isEqualTo("222");
    }

    @Test
    void liderEditaContatoDePessoaComObservacoesSemEnviarCamposPastorais() {
        pessoa.setObservacoes("nota pastoral");
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());

        PessoaUpdateRequest request = new PessoaUpdateRequest(
                "João", null, "joao@email.com", "11888888888", null, null, null, null);

        PessoaResponse response = pessoaService.atualizar(pessoa.getId(), request);

        assertThat(response.telefone()).isEqualTo("11888888888");
        assertThat(response.observacoes()).isEqualTo("nota pastoral");
        assertThat(response.ativo()).isTrue();
    }

    @Test
    void nomeEmBrancoNaAtualizacaoEhRejeitado() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));

        PessoaUpdateRequest request = new PessoaUpdateRequest(
                "   ", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> pessoaService.atualizar(pessoa.getId(), request))
                .isInstanceOf(BusinessException.class);
        verify(pessoaRepository, never()).save(any());
    }

    // --- edição de contato sincroniza o login da conta (quem gere contas) ---

    private Usuario contaDaPessoa(StatusUsuario status, String... papeisSistema) {
        Usuario usuario = usuarioCom(pessoa.getId(), true, papeisSistema);
        usuario.setStatus(status);
        usuario.setEmail("joao@email.com");
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.of(usuario));
        return usuario;
    }

    private void permissoes(boolean contasGerir, boolean papeisGestao) {
        lenient().when(autorizacaoService.tem(igrejaId, Capacidade.CONTAS_GERIR)).thenReturn(contasGerir);
        lenient().when(autorizacaoService.tem(igrejaId, Capacidade.PAPEIS_GESTAO_ATRIBUIR)).thenReturn(papeisGestao);
    }

    private PessoaUpdateRequest contato(String email, String telefone, String whatsapp) {
        return new PessoaUpdateRequest(null, null, email, telefone, whatsapp, null, null, null);
    }

    @Test
    void quemGereContasSincronizaEmailETelefoneDaConta() {
        Usuario usuario = contaDaPessoa(StatusUsuario.ATIVO, "MEMBRO");
        permissoes(true, false);
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByEmailIgnoreCase("novo@email.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByTelefone("5563999990000")).thenReturn(Optional.empty());

        pessoaService.atualizar(pessoa.getId(), contato(" Novo@Email.com", "11 3333-4444", "(63) 99999-0000"));

        assertThat(usuario.getEmail()).isEqualTo("novo@email.com");
        assertThat(usuario.getTelefone()).isEqualTo("5563999990000");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void semWhatsappUsaTelefoneNormalizadoNoLogin() {
        Usuario usuario = contaDaPessoa(StatusUsuario.CONVIDADO);
        permissoes(true, false);
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByTelefone("5563988887777")).thenReturn(Optional.empty());

        pessoaService.atualizar(pessoa.getId(), contato(null, "(63) 98888-7777", null));

        assertThat(usuario.getTelefone()).isEqualTo("5563988887777");
        assertThat(usuario.getEmail()).isEqualTo("joao@email.com");
    }

    @Test
    void liderEditandoContatoNaoAlteraOLogin() {
        Usuario usuario = contaDaPessoa(StatusUsuario.ATIVO, "MEMBRO");
        permissoes(false, false);
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PessoaResponse response = pessoaService.atualizar(pessoa.getId(),
                contato("atacante@email.com", null, "(63) 91111-2222"));

        assertThat(response.email()).isEqualTo("atacante@email.com");
        assertThat(usuario.getEmail()).isEqualTo("joao@email.com");
        assertThat(usuario.getTelefone()).isNull();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void secretariaNaoSincronizaLoginDeContaDePastor() {
        Usuario usuario = contaDaPessoa(StatusUsuario.ATIVO, "PASTOR");
        permissoes(true, false);
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pessoaService.atualizar(pessoa.getId(), contato("outro@email.com", null, "(63) 91111-2222"));

        assertThat(usuario.getEmail()).isEqualTo("joao@email.com");
        assertThat(usuario.getTelefone()).isNull();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void pastorSincronizaLoginDeContaDeSecretaria() {
        Usuario usuario = contaDaPessoa(StatusUsuario.ATIVO, "SECRETARIA");
        permissoes(true, true);
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByTelefone("5563911112222")).thenReturn(Optional.empty());

        pessoaService.atualizar(pessoa.getId(), contato(null, null, "(63) 91111-2222"));

        assertThat(usuario.getTelefone()).isEqualTo("5563911112222");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void saveDaSecretariaSemMudarTelefoneNaoSincronizaNumeroPlantado() {
        // Um líder plantou o WhatsApp na pessoa antes; a secretaria só troca o e-mail agora.
        pessoa.setWhatsapp("(63) 91111-2222");
        Usuario usuario = contaDaPessoa(StatusUsuario.ATIVO, "MEMBRO");
        permissoes(true, false);
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByEmailIgnoreCase("novo@email.com")).thenReturn(Optional.empty());

        pessoaService.atualizar(pessoa.getId(), contato("novo@email.com", null, "(63) 91111-2222"));

        assertThat(usuario.getEmail()).isEqualTo("novo@email.com");
        assertThat(usuario.getTelefone()).isNull();
        verify(usuarioRepository, never()).findByTelefone(any());
    }

    @Test
    void secretariaMudandoTelefoneSincroniza() {
        pessoa.setWhatsapp("(63) 91111-2222");
        Usuario usuario = contaDaPessoa(StatusUsuario.ATIVO, "MEMBRO");
        permissoes(true, false);
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByTelefone("5563933334444")).thenReturn(Optional.empty());

        pessoaService.atualizar(pessoa.getId(), contato(null, null, "(63) 93333-4444"));

        assertThat(usuario.getTelefone()).isEqualTo("5563933334444");
        assertThat(usuario.getEmail()).isEqualTo("joao@email.com");
    }

    @Test
    void telefoneDeOutraContaNaSincronizacaoLanca409() {
        contaDaPessoa(StatusUsuario.ATIVO, "MEMBRO");
        permissoes(true, false);
        Usuario outro = new Usuario();
        outro.setId(UUID.randomUUID());
        when(usuarioRepository.findByTelefone("5563911112222")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> pessoaService.atualizar(pessoa.getId(), contato(null, null, "(63) 91111-2222")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Telefone já usado por outra conta");
        verify(pessoaRepository, never()).save(any());
    }

    @Test
    void emailDeOutraContaNaSincronizacaoLanca409() {
        contaDaPessoa(StatusUsuario.ATIVO, "MEMBRO");
        permissoes(true, false);
        Usuario outro = new Usuario();
        outro.setId(UUID.randomUUID());
        when(usuarioRepository.findByEmailIgnoreCase("novo@email.com")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> pessoaService.atualizar(pessoa.getId(), contato("novo@email.com", null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("E-mail já usado por outra conta");
        verify(pessoaRepository, never()).save(any());
    }

    @Test
    void edicaoSemContatoNaoMexeNoLogin() {
        Usuario usuario = contaDaPessoa(StatusUsuario.ATIVO, "MEMBRO");
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pessoaService.atualizar(pessoa.getId(), new PessoaUpdateRequest(
                "João Novo", null, null, null, null, null, null, null));

        assertThat(usuario.getEmail()).isEqualTo("joao@email.com");
        verify(usuarioRepository, never()).save(any());
        verify(autorizacaoService, never()).tem(igrejaId, Capacidade.CONTAS_GERIR);
    }

    // --- excluir pessoa com conta ---

    @Test
    void excluirPessoaComContaConvidadaApagaAContaAntes() {
        Usuario usuario = contaDaPessoa(StatusUsuario.CONVIDADO);

        pessoaService.excluir(pessoa.getId());

        org.mockito.InOrder ordem = org.mockito.Mockito.inOrder(usuarioRepository, pessoaRepository);
        ordem.verify(usuarioRepository).delete(usuario);
        ordem.verify(usuarioRepository).flush();
        ordem.verify(pessoaRepository).delete(pessoa);
        verify(auditService).registrar(org.mockito.ArgumentMatchers.eq("EXCLUIR_USUARIO"),
                org.mockito.ArgumentMatchers.eq("usuario"),
                org.mockito.ArgumentMatchers.contains(usuario.getId().toString()));
    }

    @Test
    void excluirPessoaComContaDesativadaApagaAConta() {
        Usuario usuario = contaDaPessoa(StatusUsuario.DESATIVADO, "MEMBRO");

        pessoaService.excluir(pessoa.getId());

        verify(usuarioRepository).delete(usuario);
        verify(pessoaRepository).delete(pessoa);
    }

    @Test
    void excluirPessoaComContaAtivaLanca409() {
        contaDaPessoa(StatusUsuario.ATIVO, "MEMBRO");

        assertThatThrownBy(() -> pessoaService.excluir(pessoa.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Esta pessoa tem acesso ativo ao app. Desative o acesso antes de excluir.");
        verify(usuarioRepository, never()).delete(any());
        verify(pessoaRepository, never()).delete(any());
    }

    @Test
    void excluirPessoaComContaDesativadaDePastorExigePapeisGestao() {
        contaDaPessoa(StatusUsuario.DESATIVADO, "PASTOR");
        doThrow(new AccessDeniedException("Somente o pastor"))
                .when(autorizacaoService).assertPodeAtribuirPapeisGestao(igrejaId);

        assertThatThrownBy(() -> pessoaService.excluir(pessoa.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verify(usuarioRepository, never()).delete(any());
        verify(pessoaRepository, never()).delete(any());
    }

    @Test
    void excluirPessoaSemContaApagaSoAPessoa() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());

        pessoaService.excluir(pessoa.getId());

        verify(pessoaRepository).delete(pessoa);
        verify(usuarioRepository, never()).delete(any());
        verify(auditService, never()).registrar(any(), any(), any());
    }
}
