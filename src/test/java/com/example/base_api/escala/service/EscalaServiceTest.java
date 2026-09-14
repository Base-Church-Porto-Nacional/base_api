package com.example.base_api.escala.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.cronograma.service.CronogramaService;
import com.example.base_api.escala.dto.EscalaCreateRequest;
import com.example.base_api.escala.dto.EscalaResponse;
import com.example.base_api.escala.dto.EscalaStatusRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioCreateRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioRespostaRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioStatusRequest;
import com.example.base_api.escala.dto.GeracaoEscalaResponse;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaServico;
import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.entity.StatusEscala;
import com.example.base_api.escala.entity.StatusEscalaVoluntario;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.service.IgrejaContextoService;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.service.ServicoService;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscalaServiceTest {

    @Mock
    private EscalaRepository escalaRepository;
    @Mock
    private EscalaServicoRepository escalaServicoRepository;
    @Mock
    private EscalaVoluntarioRepository escalaVoluntarioRepository;
    @Mock
    private ServicoService servicoService;
    @Mock
    private PessoaService pessoaService;
    @Mock
    private PessoaServicoRepository pessoaServicoRepository;
    @Mock
    private CronogramaService cronogramaService;
    @Mock
    private IgrejaContextoService igrejaContextoService;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private MinisterioRepository ministerioRepository;
    @Mock
    private GeracaoEscalaService geracaoEscalaService;

    @InjectMocks
    private EscalaService escalaService;

    private UUID escalaId;
    private UUID servicoId;
    private UUID ministerioId;
    private UUID pessoaId;
    private UUID igrejaId;
    private UUID pastorId;

    @BeforeEach
    void setUp() {
        escalaId = UUID.randomUUID();
        servicoId = UUID.randomUUID();
        ministerioId = UUID.randomUUID();
        pessoaId = UUID.randomUUID();
        igrejaId = UUID.randomUUID();
        pastorId = UUID.randomUUID();
    }

    @Test
    void deveCriarEscalaMensalComServicos() {
        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setPastorPessoaId(pastorId);

        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja);
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(ministerioRepository.findById(ministerioId)).thenReturn(Optional.of(ministerio()));
        when(escalaRepository.save(any(Escala.class))).thenAnswer(invocation -> {
            Escala e = invocation.getArgument(0);
            e.setId(escalaId);
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });
        when(escalaServicoRepository.save(any(EscalaServico.class))).thenAnswer(invocation -> {
            EscalaServico es = invocation.getArgument(0);
            es.setId(UUID.randomUUID());
            return es;
        });
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of(escalaServico()));
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(pessoaService.buscarEntidade(pastorId)).thenReturn(pastor());
        when(geracaoEscalaService.gerar(any(Escala.class)))
                .thenReturn(new GeracaoEscalaResponse(escalaId, "SEM_EVENTOS", "Sem eventos", List.of()));

        EscalaResponse response = escalaService.criar(new EscalaCreateRequest(
                "Culto de Domingo",
                null,
                "2026-08",
                TipoEscala.CULTO,
                igrejaId,
                null,
                null,
                null,
                null,
                null,
                null,
                "Templo",
                null,
                List.of(new EscalaCreateRequest.EscalaServicoRequest(servicoId, 1)),
                List.of()
        ));

        assertThat(response.titulo()).isEqualTo("Culto de Domingo");
        assertThat(response.competencia()).isEqualTo("2026-08");
        assertThat(response.tipo()).isEqualTo(TipoEscala.CULTO);
        assertThat(response.supervisorPessoaId()).isEqualTo(pastorId);
        assertThat(response.servicos()).hasSize(1);
    }

    @Test
    void naoDeveDesignarPessoaSemQualificacao() {
        Escala escala = escala();
        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(pessoa());
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, servicoId))
                .thenReturn(Optional.of(escalaServico()));
        when(pessoaServicoRepository.findByPessoaIdAndServicoId(pessoaId, servicoId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> escalaService.designarVoluntario(
                escalaId,
                new EscalaVoluntarioCreateRequest(pessoaId, servicoId, null)
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está habilitada");
    }

    @Test
    void deveDesignarVoluntarioQuandoQualificado() {
        Escala escala = escala();
        PessoaServico vinculo = new PessoaServico();
        vinculo.setAtivo(true);

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(pessoa());
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, servicoId))
                .thenReturn(Optional.of(escalaServico()));
        when(pessoaServicoRepository.findByPessoaIdAndServicoId(pessoaId, servicoId))
                .thenReturn(Optional.of(vinculo));
        when(escalaVoluntarioRepository.findByEscalaIdAndPessoaIdAndServicoId(escalaId, pessoaId, servicoId))
                .thenReturn(Optional.empty());
        when(escalaVoluntarioRepository.countByEscalaIdAndServicoIdAndStatusIn(eq(escalaId), eq(servicoId), anyList()))
                .thenReturn(0L);
        when(escalaVoluntarioRepository.save(any(EscalaVoluntario.class))).thenAnswer(invocation -> {
            EscalaVoluntario ev = invocation.getArgument(0);
            ev.setId(UUID.randomUUID());
            ev.setCreatedAt(Instant.now());
            ev.setUpdatedAt(Instant.now());
            return ev;
        });

        EscalaResponse.EscalaVoluntarioResponse response = escalaService.designarVoluntario(
                escalaId,
                new EscalaVoluntarioCreateRequest(pessoaId, servicoId, null)
        );

        assertThat(response.pessoaId()).isEqualTo(pessoaId);
        assertThat(response.status().name()).isEqualTo("PENDENTE");
    }

    private Escala escala() {
        Escala escala = new Escala();
        escala.setId(escalaId);
        escala.setTitulo("Culto");
        escala.setDataEvento(LocalDate.of(2026, 8, 24));
        escala.setHoraInicio(LocalTime.of(18, 0));
        escala.setIgrejaId(igrejaId);
        escala.setCompetencia("2026-08");
        escala.setTipo(TipoEscala.CULTO);
        escala.setAtivo(true);
        escala.setCreatedAt(Instant.now());
        escala.setUpdatedAt(Instant.now());
        return escala;
    }

    private EscalaServico escalaServico() {
        EscalaServico es = new EscalaServico();
        es.setId(UUID.randomUUID());
        es.setEscalaId(escalaId);
        es.setServicoId(servicoId);
        es.setQuantidadeNecessaria(1);
        return es;
    }

    private Servico servico() {
        Servico servico = new Servico();
        servico.setId(servicoId);
        servico.setNome("Violão");
        servico.setMinisterioId(ministerioId);
        servico.setFiccao(false);
        return servico;
    }

    private com.example.base_api.ministerio.entity.Ministerio ministerio() {
        com.example.base_api.ministerio.entity.Ministerio ministerio =
                new com.example.base_api.ministerio.entity.Ministerio();
        ministerio.setId(ministerioId);
        ministerio.setNome("Louvor");
        ministerio.setIgrejaId(igrejaId);
        return ministerio;
    }

    private Pessoa pessoa() {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(pessoaId);
        pessoa.setNome("Gustavo");
        pessoa.setIgrejaId(igrejaId);
        return pessoa;
    }

    private Pessoa pastor() {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(pastorId);
        pessoa.setNome("Ana Costa");
        pessoa.setIgrejaId(igrejaId);
        return pessoa;
    }

    @Test
    void validaJanelaDeAberturaNoDia20DoMesAnterior() {
        assertThat(escalaService.janelaAberturaAberta("2026-09", LocalDate.of(2026, 8, 19))).isFalse();
        assertThat(escalaService.janelaAberturaAberta("2026-09", LocalDate.of(2026, 8, 20))).isTrue();
        assertThat(escalaService.janelaAberturaAberta("2026-09", LocalDate.of(2026, 9, 5))).isTrue();
        assertThat(escalaService.janelaAberturaAberta("2026-08", LocalDate.of(2026, 7, 20))).isTrue();
    }

    @Test
    void deveLancarEscalaAoTransitarParaEmAndamento() {
        Escala escala = escala();
        escala.setStatus(StatusEscala.EDITANDO);
        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaRepository.save(any(Escala.class))).thenAnswer(inv -> inv.getArgument(0));
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());

        EscalaResponse response = escalaService.atualizarStatus(
                escalaId, new EscalaStatusRequest(StatusEscala.EM_ANDAMENTO));

        assertThat(response.status()).isEqualTo(StatusEscala.EM_ANDAMENTO);
        assertThat(response.dataLancamento()).isNotNull();
    }

    @Test
    void naoDeveVoltarDeFinalizadaParaSugerida() {
        Escala escala = escala();
        escala.setStatus(StatusEscala.FINALIZADA);
        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));

        assertThatThrownBy(() -> escalaService.atualizarStatus(
                escalaId, new EscalaStatusRequest(StatusEscala.SUGERIDA)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");
    }

    @Test
    void deveConfirmarPropriaDesignacaoAposEscalaLancada() {
        Escala escala = escala();
        escala.setStatus(StatusEscala.EM_ANDAMENTO);
        EscalaVoluntario designacao = designacao();
        Usuario usuario = usuarioLogado(pessoaId);

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaVoluntarioRepository.findById(designacao.getId())).thenReturn(Optional.of(designacao));
        when(autorizacaoService.requireUsuario()).thenReturn(usuario);
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(pessoa());
        when(escalaVoluntarioRepository.save(any(EscalaVoluntario.class))).thenAnswer(inv -> inv.getArgument(0));

        EscalaResponse.EscalaVoluntarioResponse response = escalaService.responder(
                escalaId,
                designacao.getId(),
                new EscalaVoluntarioRespostaRequest(StatusEscalaVoluntario.CONFIRMADO, "Confirmo a minha participação")
        );

        assertThat(response.status()).isEqualTo(StatusEscalaVoluntario.CONFIRMADO);
        assertThat(response.confirmadoPeloVoluntario()).isTrue();
        assertThat(response.observacoes()).isEqualTo("Confirmo a minha participação");
    }

    @Test
    void naoDeveResponderDesignacaoDeOutraPessoa() {
        Escala escala = escala();
        escala.setStatus(StatusEscala.EM_ANDAMENTO);
        EscalaVoluntario designacao = designacao();
        designacao.setPessoaId(UUID.randomUUID());

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaVoluntarioRepository.findById(designacao.getId())).thenReturn(Optional.of(designacao));
        when(autorizacaoService.requireUsuario()).thenReturn(usuarioLogado(pessoaId));

        assertThatThrownBy(() -> escalaService.responder(
                escalaId,
                designacao.getId(),
                new EscalaVoluntarioRespostaRequest(StatusEscalaVoluntario.CONFIRMADO, null)
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("própria designação");
    }

    @Test
    void naoDeveResponderDesignacaoAntesDaEscalaSerLancada() {
        Escala escala = escala();
        escala.setStatus(StatusEscala.EDITANDO);
        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));

        assertThatThrownBy(() -> escalaService.responder(
                escalaId,
                UUID.randomUUID(),
                new EscalaVoluntarioRespostaRequest(StatusEscalaVoluntario.CONFIRMADO, null)
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("escala ser lançada");
    }

    @Test
    void deveExigirMotivoNaRecusa() {
        Escala escala = escala();
        escala.setStatus(StatusEscala.EM_ANDAMENTO);
        EscalaVoluntario designacao = designacao();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaVoluntarioRepository.findById(designacao.getId())).thenReturn(Optional.of(designacao));
        when(autorizacaoService.requireUsuario()).thenReturn(usuarioLogado(pessoaId));

        assertThatThrownBy(() -> escalaService.responder(
                escalaId,
                designacao.getId(),
                new EscalaVoluntarioRespostaRequest(StatusEscalaVoluntario.RECUSADO, " ")
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("motivo da recusa");
    }

    @Test
    void liderDeveRegistrarAprovacaoEmNomeDoVoluntario() {
        Escala escala = escala();
        EscalaVoluntario designacao = designacao();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaVoluntarioRepository.findById(designacao.getId())).thenReturn(Optional.of(designacao));
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(pessoa());
        when(escalaVoluntarioRepository.save(any(EscalaVoluntario.class))).thenAnswer(inv -> inv.getArgument(0));

        EscalaResponse.EscalaVoluntarioResponse response = escalaService.atualizarStatus(
                escalaId,
                designacao.getId(),
                new EscalaVoluntarioStatusRequest(
                        StatusEscalaVoluntario.CONFIRMADO,
                        "Confirmado pelo líder, o voluntário não tem acesso ao sistema",
                        true
                )
        );

        assertThat(response.status()).isEqualTo(StatusEscalaVoluntario.CONFIRMADO);
        assertThat(response.confirmadoPeloVoluntario()).isTrue();
    }

    @Test
    void liderDeveConfirmarSemMarcarComoAprovadoPeloVoluntario() {
        Escala escala = escala();
        EscalaVoluntario designacao = designacao();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaVoluntarioRepository.findById(designacao.getId())).thenReturn(Optional.of(designacao));
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(pessoa());
        when(escalaVoluntarioRepository.save(any(EscalaVoluntario.class))).thenAnswer(inv -> inv.getArgument(0));

        EscalaResponse.EscalaVoluntarioResponse response = escalaService.atualizarStatus(
                escalaId,
                designacao.getId(),
                new EscalaVoluntarioStatusRequest(StatusEscalaVoluntario.CONFIRMADO, null, null)
        );

        assertThat(response.status()).isEqualTo(StatusEscalaVoluntario.CONFIRMADO);
        assertThat(response.confirmadoPeloVoluntario()).isFalse();
    }

    private EscalaVoluntario designacao() {
        EscalaVoluntario designacao = new EscalaVoluntario();
        designacao.setId(UUID.randomUUID());
        designacao.setEscalaId(escalaId);
        designacao.setPessoaId(pessoaId);
        designacao.setServicoId(servicoId);
        designacao.setStatus(StatusEscalaVoluntario.PENDENTE);
        designacao.setConfirmadoPeloVoluntario(false);
        designacao.setCreatedAt(Instant.now());
        designacao.setUpdatedAt(Instant.now());
        return designacao;
    }

    private Usuario usuarioLogado(UUID pessoaLogadaId) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setPessoaId(pessoaLogadaId);
        usuario.setUsername("voluntario");
        usuario.setEmail("voluntario@igreja.com");
        usuario.setSenhaHash("hash");
        return usuario;
    }
}
