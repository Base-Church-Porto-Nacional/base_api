package com.example.base_api.igreja.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.baseministerial.repository.BaseMinisterialRepository;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.common.exception.EntidadeNaoProcessavelException;
import com.example.base_api.igreja.dto.FilialCriadaResponse;
import com.example.base_api.igreja.dto.IgrejaFilialCreateRequest;
import com.example.base_api.igreja.dto.IgrejaResponse;
import com.example.base_api.igreja.dto.PastorFilialRequest;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.entity.IgrejaTipo;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.jornada.service.JornadaService;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.smallgroup.repository.SmallGroupRepository;
import com.example.base_api.usuario.dto.LinkAcessoResponse;
import com.example.base_api.usuario.entity.StatusUsuario;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import com.example.base_api.usuario.service.AcessoService;
import com.example.base_api.usuario.service.UsuarioService;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IgrejaServiceTest {

    @Mock
    private IgrejaRepository igrejaRepository;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private IgrejaContextoService igrejaContextoService;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private JornadaService jornadaService;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private AcessoService acessoService;
    @Mock
    private AuditService auditService;
    @Mock
    private MinisterioRepository ministerioRepository;
    @Mock
    private BaseMinisterialRepository baseMinisterialRepository;
    @Mock
    private SmallGroupRepository smallGroupRepository;
    @Mock
    private PessoaServicoRepository pessoaServicoRepository;

    private IgrejaService igrejaService;

    @BeforeEach
    void setUp() {
        // Transferência real (com repositórios mockados) para que as regras de segurança sejam exercitadas no fluxo.
        TransferenciaPessoaService transferencia = new TransferenciaPessoaService(
                autorizacaoService, igrejaRepository, pessoaRepository, usuarioRepository, usuarioService,
                ministerioRepository, baseMinisterialRepository, smallGroupRepository, pessoaServicoRepository,
                auditService);
        igrejaService = new IgrejaService(
                igrejaRepository, pessoaRepository, usuarioRepository, igrejaContextoService, autorizacaoService,
                jornadaService, usuarioService, acessoService, transferencia, auditService);
    }

    @Test
    void deveListarIgrejasVisiveisDaMatriz() {
        Igreja matriz = igreja(IgrejaTipo.MATRIZ, null);
        Igreja filial = igreja(IgrejaTipo.FILIAL, matriz.getId());
        when(igrejaContextoService.igrejasVisiveis()).thenReturn(List.of(matriz, filial));

        List<IgrejaResponse> visiveis = igrejaService.listarVisiveis();

        assertThat(visiveis).hasSize(2);
        assertThat(visiveis.get(0).tipo()).isEqualTo(IgrejaTipo.MATRIZ);
        assertThat(visiveis.get(1).tipo()).isEqualTo(IgrejaTipo.FILIAL);
    }

    @Test
    void deveMontarResponseComPastor() {
        UUID pastorId = UUID.randomUUID();
        Igreja igreja = igreja(IgrejaTipo.MATRIZ, null);
        igreja.setPastorPessoaId(pastorId);
        when(pessoaRepository.nomesPorId(any())).thenReturn(Map.of(pastorId, "Ana Costa"));

        IgrejaResponse response = igrejaService.toResponse(igreja);

        assertThat(response.pastorPessoaId()).isEqualTo(pastorId);
        assertThat(response.pastorNome()).isEqualTo("Ana Costa");
    }

    private Igreja igreja(IgrejaTipo tipo, UUID matrizId) {
        Igreja igreja = new Igreja();
        igreja.setId(UUID.randomUUID());
        igreja.setNome(tipo == IgrejaTipo.MATRIZ ? "Base Church" : "Filial");
        igreja.setTipo(tipo);
        igreja.setIgrejaMatrizId(matrizId);
        igreja.setAtivo(true);
        igreja.setCreatedAt(Instant.now());
        igreja.setUpdatedAt(Instant.now());
        return igreja;
    }

    @Test
    void listarVisiveisBuscaPastoresEmLote() {
        Pessoa pastora = new Pessoa();
        pastora.setId(UUID.randomUUID());
        pastora.setNome("Pra. Ana");
        Igreja sede = igrejaDeTeste("Sede");
        sede.setPastorPessoaId(pastora.getId());
        Igreja filial = igrejaDeTeste("Filial");

        when(igrejaContextoService.igrejasVisiveis()).thenReturn(List.of(sede, filial));
        when(pessoaRepository.nomesPorId(any())).thenReturn(Map.of(pastora.getId(), "Pra. Ana"));

        List<IgrejaResponse> igrejas = igrejaService.listarVisiveis();

        assertThat(igrejas).extracting(IgrejaResponse::pastorNome).containsExactly("Pra. Ana", null);
        verify(pessoaRepository, never()).findById(any());
    }

    private Igreja igrejaDeTeste(String nome) {
        Igreja igreja = new Igreja();
        igreja.setId(UUID.randomUUID());
        igreja.setNome(nome);
        igreja.setTipo(IgrejaTipo.FILIAL);
        igreja.setAtivo(true);
        return igreja;
    }

    // --- troca de pastor da igreja ---

    private Pessoa pessoaDa(UUID igrejaId) {
        Pessoa p = new Pessoa();
        p.setId(UUID.randomUUID());
        p.setNome("Pr. Novo");
        p.setIgrejaId(igrejaId);
        return p;
    }

    @Test
    void secretariaNaoTrocaPastorDaIgreja() {
        Igreja igreja = igreja(IgrejaTipo.MATRIZ, null);
        igreja.setPastorPessoaId(UUID.randomUUID());
        Pessoa novo = pessoaDa(igreja.getId());
        when(igrejaRepository.findById(igreja.getId())).thenReturn(Optional.of(igreja));
        doThrow(new AccessDeniedException("Sem permissão para esta ação"))
                .when(autorizacaoService).assertTem(igreja.getId(), com.example.base_api.security.Capacidade.FILIAL_CRIAR);

        assertThatThrownBy(() -> igrejaService.atualizar(igreja.getId(),
                new com.example.base_api.igreja.dto.IgrejaUpdateRequest("Base", novo.getId(), true)))
                .isInstanceOf(AccessDeniedException.class);
        verify(igrejaRepository, never()).save(any());
    }

    @Test
    void secretariaAtualizaNomeMantendoPastor() {
        Igreja igreja = igreja(IgrejaTipo.MATRIZ, null);
        Pessoa pastor = pessoaDa(igreja.getId());
        igreja.setPastorPessoaId(pastor.getId());
        when(igrejaRepository.findById(igreja.getId())).thenReturn(Optional.of(igreja));
        when(igrejaRepository.save(any(Igreja.class))).thenAnswer(i -> i.getArgument(0));

        igrejaService.atualizar(igreja.getId(),
                new com.example.base_api.igreja.dto.IgrejaUpdateRequest("Novo nome", pastor.getId(), true));

        assertThat(igreja.getNome()).isEqualTo("Novo nome");
        verify(autorizacaoService, never()).assertTem(any(), any());
    }

    @Test
    void pastorDeOutraIgrejaNaoPodeSerDefinido() {
        Igreja igreja = igreja(IgrejaTipo.MATRIZ, null);
        igreja.setPastorPessoaId(UUID.randomUUID());
        Pessoa alheio = pessoaDa(UUID.randomUUID());
        when(igrejaRepository.findById(igreja.getId())).thenReturn(Optional.of(igreja));
        when(pessoaRepository.findById(alheio.getId())).thenReturn(Optional.of(alheio));

        assertThatThrownBy(() -> igrejaService.atualizar(igreja.getId(),
                new com.example.base_api.igreja.dto.IgrejaUpdateRequest("Base", alheio.getId(), true)))
                .isInstanceOf(com.example.base_api.common.exception.BusinessException.class)
                .hasMessage("A pessoa não pertence a esta igreja");
        verify(igrejaRepository, never()).save(any());
    }

    @Test
    void pastorTrocaPastorPorPessoaDaIgreja() {
        Igreja igreja = igreja(IgrejaTipo.MATRIZ, null);
        igreja.setPastorPessoaId(UUID.randomUUID());
        Pessoa novo = pessoaDa(igreja.getId());
        when(igrejaRepository.findById(igreja.getId())).thenReturn(Optional.of(igreja));
        when(pessoaRepository.findById(novo.getId())).thenReturn(Optional.of(novo));
        when(igrejaRepository.save(any(Igreja.class))).thenAnswer(i -> i.getArgument(0));

        igrejaService.atualizar(igreja.getId(),
                new com.example.base_api.igreja.dto.IgrejaUpdateRequest("Base", novo.getId(), true));

        assertThat(igreja.getPastorPessoaId()).isEqualTo(novo.getId());
        verify(autorizacaoService).assertTem(igreja.getId(), com.example.base_api.security.Capacidade.FILIAL_CRIAR);
    }

    // --- criar filial com pastor ---

    private Igreja matriz;

    private Igreja stubMatriz() {
        matriz = igreja(IgrejaTipo.MATRIZ, null);
        when(igrejaRepository.findById(matriz.getId())).thenReturn(Optional.of(matriz));
        return matriz;
    }

    private void stubSaves() {
        when(igrejaRepository.save(any(Igreja.class))).thenAnswer(i -> {
            Igreja ig = i.getArgument(0);
            if (ig.getId() == null) {
                ig.setId(UUID.randomUUID());
            }
            return ig;
        });
    }

    private Pessoa pessoaExistente(UUID igrejaId) {
        Pessoa p = pessoaDa(igrejaId);
        p.setWhatsapp("(11) 98888-7777");
        when(pessoaRepository.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    private Usuario usuarioDe(Pessoa pessoa, StatusUsuario status) {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setPessoaId(pessoa.getId());
        u.setIgrejaId(pessoa.getIgrejaId());
        u.setStatus(status);
        u.setTokenVersion(0);
        return u;
    }

    private static IgrejaFilialCreateRequest pedidoExistente(UUID pessoaId) {
        return new IgrejaFilialCreateRequest("Filial Norte", new PastorFilialRequest(pessoaId, null, null, null));
    }

    @Test
    void criaFilialComPessoaNovaEConvite() {
        stubMatriz();
        stubSaves();
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(i -> {
            Pessoa p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        Usuario convidado = new Usuario();
        convidado.setId(UUID.randomUUID());
        convidado.setStatus(StatusUsuario.CONVIDADO);
        when(acessoService.convidar(any())).thenAnswer(i -> {
            convidado.setPessoaId(i.getArgument(0));
            return new LinkAcessoResponse("https://app/convite?token=abc", true);
        });
        when(usuarioRepository.findByPessoaId(any())).thenAnswer(i -> Optional.of(convidado));

        FilialCriadaResponse resposta = igrejaService.criarFilial(matriz.getId(), new IgrejaFilialCreateRequest(
                " Filial Norte ", new PastorFilialRequest(null, " Pr. Novo ", "Novo@Base.church", null)));

        org.mockito.ArgumentCaptor<Pessoa> pessoaCaptor = org.mockito.ArgumentCaptor.forClass(Pessoa.class);
        verify(pessoaRepository).save(pessoaCaptor.capture());
        Pessoa nova = pessoaCaptor.getValue();
        UUID filialId = resposta.igreja().id();
        assertThat(resposta.igreja().tipo()).isEqualTo(IgrejaTipo.FILIAL);
        assertThat(resposta.igreja().nome()).isEqualTo("Filial Norte");
        assertThat(resposta.igreja().igrejaMatrizId()).isEqualTo(matriz.getId());
        assertThat(resposta.igreja().pastorPessoaId()).isEqualTo(nova.getId());
        assertThat(nova.getIgrejaId()).isEqualTo(filialId);
        assertThat(nova.getNome()).isEqualTo("Pr. Novo");
        assertThat(nova.getEmail()).isEqualTo("novo@base.church");
        verify(jornadaService).criarJornadaInicial(nova.getId());
        verify(acessoService).convidar(nova.getId());
        verify(usuarioService).substituirPapeis(convidado, filialId, Set.of("PASTOR"));
        assertThat(resposta.convite()).isNotNull();
        assertThat(resposta.convite().link()).isEqualTo("https://app/convite?token=abc");
        verify(auditService).registrar(eq("CRIAR_FILIAL"), eq("igreja"), anyString());
    }

    @Test
    void criaFilialComPessoaExistenteComContaSemConvite() {
        stubMatriz();
        stubSaves();
        Pessoa pessoa = pessoaExistente(matriz.getId());
        Usuario usuario = usuarioDe(pessoa, StatusUsuario.ATIVO);
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.of(usuario));

        FilialCriadaResponse resposta = igrejaService.criarFilial(matriz.getId(), pedidoExistente(pessoa.getId()));

        UUID filialId = resposta.igreja().id();
        assertThat(resposta.convite()).isNull();
        assertThat(resposta.igreja().pastorPessoaId()).isEqualTo(pessoa.getId());
        assertThat(pessoa.getIgrejaId()).isEqualTo(filialId);
        assertThat(usuario.getIgrejaId()).isEqualTo(filialId);
        assertThat(usuario.getTokenVersion()).isEqualTo(1);
        verify(usuarioService).substituirPapeis(usuario, matriz.getId(), Set.of());
        verify(usuarioService).substituirPapeis(usuario, filialId, Set.of("PASTOR"));
        verify(acessoService, never()).convidar(any());
        verify(pessoaRepository, never()).save(org.mockito.ArgumentMatchers.argThat(p -> p.getId() == null));
        verify(auditService).registrar(eq("TRANSFERIR_PESSOA"), eq("pessoa"), anyString());
        verify(auditService).registrar(eq("CRIAR_FILIAL"), eq("igreja"), anyString());
    }

    @Test
    void pessoaExistenteComContaConvidadaRecebeNovoConviteEPapelPastor() {
        stubMatriz();
        stubSaves();
        Pessoa pessoa = pessoaExistente(matriz.getId());
        Usuario usuario = usuarioDe(pessoa, StatusUsuario.CONVIDADO);
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.of(usuario));
        when(acessoService.convidar(pessoa.getId())).thenReturn(new LinkAcessoResponse("https://app/c", false));

        FilialCriadaResponse resposta = igrejaService.criarFilial(matriz.getId(), pedidoExistente(pessoa.getId()));

        assertThat(resposta.convite().link()).isEqualTo("https://app/c");
        assertThat(usuario.getIgrejaId()).isEqualTo(resposta.igreja().id());
        verify(usuarioService).substituirPapeis(usuario, resposta.igreja().id(), Set.of("PASTOR"));
    }

    @Test
    void pessoaQueLideraMinisterioNaoETransferida() {
        stubMatriz();
        Pessoa pessoa = pessoaExistente(matriz.getId());
        when(ministerioRepository.findLideradosOuCoordenados(matriz.getId(), pessoa.getId()))
                .thenReturn(List.of(new Ministerio()));

        assertThatThrownBy(() -> igrejaService.criarFilial(matriz.getId(), pedidoExistente(pessoa.getId())))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Esta pessoa lidera ministérios ou grupos na igreja atual. Troque a liderança antes de transferir.");
        verify(igrejaRepository, never()).save(any());
        assertThat(pessoa.getIgrejaId()).isEqualTo(matriz.getId());
    }

    @Test
    void pastorTitularDaMatrizNaoETransferido() {
        stubMatriz();
        Pessoa pessoa = pessoaExistente(matriz.getId());
        matriz.setPastorPessoaId(pessoa.getId());
        when(igrejaRepository.existsByPastorPessoaId(pessoa.getId())).thenReturn(true);

        assertThatThrownBy(() -> igrejaService.criarFilial(matriz.getId(), pedidoExistente(pessoa.getId())))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Esta pessoa é o pastor titular da igreja atual.");
        verify(igrejaRepository, never()).save(any());
    }

    @Test
    void pastorSemContatoLanca422SemCriarFilial() {
        stubMatriz();

        assertThatThrownBy(() -> igrejaService.criarFilial(matriz.getId(), new IgrejaFilialCreateRequest(
                "Filial Norte", new PastorFilialRequest(null, "Pr. Novo", null, " "))))
                .isInstanceOf(EntidadeNaoProcessavelException.class)
                .hasMessage("Cadastre e-mail ou WhatsApp antes de convidar");
        verify(igrejaRepository, never()).save(any());
        verify(pessoaRepository, never()).save(any());
    }

    @Test
    void pessoaExistenteSemContaESemContatoLanca422SemCriarFilial() {
        stubMatriz();
        Pessoa pessoa = pessoaDa(matriz.getId());
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> igrejaService.criarFilial(matriz.getId(), pedidoExistente(pessoa.getId())))
                .isInstanceOf(EntidadeNaoProcessavelException.class)
                .hasMessage("Cadastre e-mail ou WhatsApp antes de convidar");
        verify(igrejaRepository, never()).save(any());
    }

    @Test
    void pastorComContaDesativadaLanca409() {
        stubMatriz();
        Pessoa pessoa = pessoaExistente(matriz.getId());
        when(usuarioRepository.findByPessoaId(pessoa.getId()))
                .thenReturn(Optional.of(usuarioDe(pessoa, StatusUsuario.DESATIVADO)));

        assertThatThrownBy(() -> igrejaService.criarFilial(matriz.getId(), pedidoExistente(pessoa.getId())))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A conta desta pessoa está desativada. Reative antes de torná-la pastor.");
        verify(igrejaRepository, never()).save(any());
    }

    @Test
    void pedidoComPessoaIdENomeLanca400() {
        stubMatriz();

        assertThatThrownBy(() -> igrejaService.criarFilial(matriz.getId(), new IgrejaFilialCreateRequest(
                "Filial Norte", new PastorFilialRequest(UUID.randomUUID(), "Pr. Novo", "a@b.com", null))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Informe uma pessoa existente ou os dados de uma nova pessoa");
        verify(igrejaRepository, never()).save(any());
    }

    @Test
    void pedidoSemPessoaIdNemNomeLanca400() {
        stubMatriz();

        assertThatThrownBy(() -> igrejaService.criarFilial(matriz.getId(), new IgrejaFilialCreateRequest(
                "Filial Norte", new PastorFilialRequest(null, " ", "a@b.com", null))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Informe uma pessoa existente ou os dados de uma nova pessoa");
        verify(igrejaRepository, never()).save(any());
    }

    @Test
    void pastorDeFilialNaoCriaFilial() {
        doThrow(new AccessDeniedException("Somente o pastor da matriz pode criar filial"))
                .when(autorizacaoService).assertPodeCriarFilial();

        assertThatThrownBy(() -> igrejaService.criarFilial(UUID.randomUUID(), pedidoExistente(UUID.randomUUID())))
                .isInstanceOf(AccessDeniedException.class);
        verify(igrejaRepository, never()).save(any());
    }

    @Test
    void conflitoDeTelefoneLanca409SemCriarFilial() {
        stubMatriz();
        Usuario outro = new Usuario();
        outro.setId(UUID.randomUUID());
        when(usuarioRepository.findByTelefone("5511988887777")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> igrejaService.criarFilial(matriz.getId(), new IgrejaFilialCreateRequest(
                "Filial Norte", new PastorFilialRequest(null, "Pr. Novo", null, "(11) 98888-7777"))))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Telefone já usado por outra conta");
        verify(igrejaRepository, never()).save(any());
        verify(pessoaRepository, never()).save(any());
    }

    @Test
    void conflitoDeEmailLanca409SemCriarFilial() {
        stubMatriz();
        Usuario outro = new Usuario();
        outro.setId(UUID.randomUUID());
        when(usuarioRepository.findByEmailIgnoreCase("novo@base.church")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> igrejaService.criarFilial(matriz.getId(), new IgrejaFilialCreateRequest(
                "Filial Norte", new PastorFilialRequest(null, "Pr. Novo", "Novo@Base.church", null))))
                .isInstanceOf(ConflictException.class)
                .hasMessage("E-mail já usado por outra conta");
        verify(igrejaRepository, never()).save(any());
    }

    @Test
    void contaConvidadaDaPropriaPessoaNaoContaComoConflito() {
        stubMatriz();
        stubSaves();
        Pessoa pessoa = pessoaExistente(matriz.getId());
        Usuario usuario = usuarioDe(pessoa, StatusUsuario.CONVIDADO);
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByTelefone("5511988887777")).thenReturn(Optional.of(usuario));
        when(acessoService.convidar(pessoa.getId())).thenReturn(new LinkAcessoResponse("https://app/c", false));

        FilialCriadaResponse resposta = igrejaService.criarFilial(matriz.getId(), pedidoExistente(pessoa.getId()));

        assertThat(resposta.convite()).isNotNull();
    }

    @Test
    void convidarFalhandoNaoAuditaTransferencia() {
        stubMatriz();
        stubSaves();
        Pessoa pessoa = pessoaExistente(matriz.getId());
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());
        when(acessoService.convidar(pessoa.getId())).thenThrow(new ConflictException("E-mail já usado por outra conta"));

        assertThatThrownBy(() -> igrejaService.criarFilial(matriz.getId(), pedidoExistente(pessoa.getId())))
                .isInstanceOf(ConflictException.class);
        verify(auditService, never()).registrar(eq("TRANSFERIR_PESSOA"), any(), any());
        verify(auditService, never()).registrar(eq("CRIAR_FILIAL"), any(), any());
    }

    @Test
    void pessoaInativaNaoViraPastor() {
        stubMatriz();
        Pessoa pessoa = pessoaExistente(matriz.getId());
        pessoa.setAtivo(false);

        assertThatThrownBy(() -> igrejaService.criarFilial(matriz.getId(), pedidoExistente(pessoa.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Esta pessoa está inativa.");
        verify(igrejaRepository, never()).save(any());
    }
}
