package com.example.base_api.escala.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.cronograma.service.CronogramaService;
import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.entity.StatusCulto;
import com.example.base_api.culto.repository.CultoRepository;
import com.example.base_api.escala.dto.EscalaCreateRequest;
import com.example.base_api.escala.dto.EscalaResponse;
import com.example.base_api.escala.dto.EscalaServicoUpsertRequest;
import com.example.base_api.escala.dto.EscalaStatusRequest;
import com.example.base_api.escala.dto.EscalaUpdateRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioCreateRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioRespostaRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioStatusRequest;
import com.example.base_api.escala.dto.EscalaVoluntarioUpdateRequest;
import com.example.base_api.escala.dto.GeracaoEscalaResponse;
import com.example.base_api.escala.dto.VisaoMensalEscalaResponse;
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
import com.example.base_api.ministerio.entity.Ministerio;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    private CronogramaEventoRepository cronogramaEventoRepository;
    @Mock
    private CultoRepository cultoRepository;
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
                new EscalaVoluntarioCreateRequest(pessoaId, servicoId, null, null, null, null)
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
                .thenReturn(List.of());
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
                new EscalaVoluntarioCreateRequest(pessoaId, servicoId, null, null, null, null)
        );

        assertThat(response.pessoaId()).isEqualTo(pessoaId);
        assertThat(response.status().name()).isEqualTo("PENDENTE");
    }

    @Test
    void naoDeveDesignarSemDataQuandoPessoaJaTemDesignacoesEmVariasDatas() {
        Escala escala = escala();
        PessoaServico vinculo = new PessoaServico();
        vinculo.setAtivo(true);
        EscalaVoluntario dia6 = designacao();
        dia6.setDataItem(LocalDate.of(2026, 9, 6));
        EscalaVoluntario dia13 = designacao();
        dia13.setId(UUID.randomUUID());
        dia13.setDataItem(LocalDate.of(2026, 9, 13));

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(pessoa());
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, servicoId))
                .thenReturn(Optional.of(escalaServico()));
        when(pessoaServicoRepository.findByPessoaIdAndServicoId(pessoaId, servicoId))
                .thenReturn(Optional.of(vinculo));
        when(escalaVoluntarioRepository.findByEscalaIdAndPessoaIdAndServicoId(escalaId, pessoaId, servicoId))
                .thenReturn(List.of(dia6, dia13));

        assertThatThrownBy(() -> escalaService.designarVoluntario(
                escalaId,
                new EscalaVoluntarioCreateRequest(pessoaId, servicoId, null, null, null, null)
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("A pessoa já está designada para este serviço nesta escala");
        verify(escalaVoluntarioRepository, never()).save(any(EscalaVoluntario.class));
    }

    @Test
    void liderDeveVerVisaoMensalComTodosOsCultosEEventosDoMes() {
        Ministerio ministerioLiderado = new Ministerio();
        ministerioLiderado.setId(ministerioId);
        ministerioLiderado.setNome("Louvor");
        ministerioLiderado.setIgrejaId(igrejaId);
        ministerioLiderado.setLiderPessoaId(pessoaId);

        CronogramaEvento evento = eventoDeReferencia();
        EscalaVoluntario designacao = designacao();
        designacao.setEventoId(evento.getId());
        designacao.setDataItem(LocalDate.of(2026, 8, 3));
        designacao.setHorario(LocalTime.of(18, 0));

        EscalaServico comDois = escalaServico();
        comDois.setQuantidadeNecessaria(2);

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(autorizacaoService.requireUsuario()).thenReturn(usuarioLogado(pessoaId));
        when(autorizacaoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(ministerioRepository.findByIgrejaIdInOrderByNomeAsc(anyList())).thenReturn(List.of(ministerioLiderado));
        when(escalaRepository.findByMinisterioIdAndCompetenciaAndAtivoTrueOrderByDataEventoAsc(
                ministerioId, "2026-08")).thenReturn(List.of(escalaMensal()));
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of(comDois));
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of(designacao));
        when(cultoRepository.filtrar(anyList(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(cultoDomingo()));
        when(cronogramaEventoRepository
                .findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(anyList(), any(), any()))
                .thenReturn(List.of(evento));
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(pessoa());

        List<VisaoMensalEscalaResponse> resultado = escalaService.visaoMensal(igrejaId, "2026-08", null);

        assertThat(resultado).hasSize(1);
        VisaoMensalEscalaResponse visao = resultado.get(0);
        assertThat(visao.ministerioId()).isEqualTo(ministerioId);
        assertThat(visao.escalaId()).isEqualTo(escalaId);
        assertThat(visao.itens()).hasSize(2);

        VisaoMensalEscalaResponse.ItemVisao cultoVisao = visao.itens().get(0);
        assertThat(cultoVisao.origem()).isEqualTo("CULTO");
        assertThat(cultoVisao.data()).isEqualTo(LocalDate.of(2026, 8, 2));
        assertThat(cultoVisao.servicos()).hasSize(1);
        assertThat(cultoVisao.servicos().get(0).servicoNome()).isEqualTo("Violão");
        assertThat(cultoVisao.servicos().get(0).quantidadeNecessaria()).isEqualTo(2);
        assertThat(cultoVisao.servicos().get(0).designados()).isEmpty();

        VisaoMensalEscalaResponse.ItemVisao eventoVisao = visao.itens().get(1);
        assertThat(eventoVisao.origem()).isEqualTo("EVENTO");
        assertThat(eventoVisao.data()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(eventoVisao.servicos()).hasSize(1);
        assertThat(eventoVisao.servicos().get(0).designados()).hasSize(1);
        assertThat(eventoVisao.servicos().get(0).designados().get(0).pessoaNome()).isEqualTo("Gustavo");
    }

    @Test
    void naoDevePermitirVisaoMensalDeMinisterioQueNaoLidera() {
        Ministerio alheio = new Ministerio();
        alheio.setId(ministerioId);
        alheio.setNome("Teatro");
        alheio.setIgrejaId(igrejaId);
        alheio.setLiderPessoaId(UUID.randomUUID());

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(autorizacaoService.requireUsuario()).thenReturn(usuarioLogado(pessoaId));
        when(autorizacaoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(ministerioRepository.findByIgrejaIdInOrderByNomeAsc(anyList())).thenReturn(List.of(alheio));

        assertThatThrownBy(() -> escalaService.visaoMensal(igrejaId, "2026-08", ministerioId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Sem permissão para ver a escala deste ministério");
    }

    @Test
    void deveDesignarVoluntarioParaDataDeUmEventoEspecifico() {
        Escala escala = escala();
        CronogramaEvento evento = new CronogramaEvento();
        evento.setId(UUID.randomUUID());
        evento.setIgrejaId(igrejaId);
        evento.setDataEvento(LocalDate.of(2026, 8, 3));
        evento.setHoraInicio(LocalTime.of(18, 0));
        evento.setCompetencia("2026-08");
        evento.setAtivo(true);

        PessoaServico vinculo = new PessoaServico();
        vinculo.setAtivo(true);

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(pessoa());
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, servicoId))
                .thenReturn(Optional.of(escalaServico()));
        when(pessoaServicoRepository.findByPessoaIdAndServicoId(pessoaId, servicoId))
                .thenReturn(Optional.of(vinculo));
        when(cronogramaService.buscarEntidade(evento.getId())).thenReturn(evento);
        when(escalaVoluntarioRepository.findByEscalaIdAndPessoaIdAndServicoIdAndDataItem(
                escalaId, pessoaId, servicoId, LocalDate.of(2026, 8, 3)))
                .thenReturn(Optional.empty());
        when(escalaVoluntarioRepository.countByEscalaIdAndServicoIdAndDataItemAndStatusIn(
                eq(escalaId), eq(servicoId), eq(LocalDate.of(2026, 8, 3)), anyList()))
                .thenReturn(0L);
        when(escalaVoluntarioRepository.save(any(EscalaVoluntario.class))).thenAnswer(invocation -> {
            EscalaVoluntario ev = invocation.getArgument(0);
            ev.setId(UUID.randomUUID());
            ev.setCreatedAt(Instant.now());
            ev.setUpdatedAt(Instant.now());
            return ev;
        });

        escalaService.designarVoluntario(
                escalaId,
                new EscalaVoluntarioCreateRequest(pessoaId, servicoId, null, null, evento.getId(), null)
        );

        ArgumentCaptor<EscalaVoluntario> captor = ArgumentCaptor.forClass(EscalaVoluntario.class);
        verify(escalaVoluntarioRepository).save(captor.capture());
        EscalaVoluntario salvo = captor.getValue();
        assertThat(salvo.getEventoId()).isEqualTo(evento.getId());
        assertThat(salvo.getDataItem()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(salvo.getHorario()).isEqualTo(LocalTime.of(18, 0));
    }

    private Escala escalaMensal() {
        Escala escala = new Escala();
        escala.setId(escalaId);
        escala.setTitulo("Louvor — agosto");
        escala.setCompetencia("2026-08");
        escala.setTipo(TipoEscala.MINISTERIO);
        escala.setMinisterioId(ministerioId);
        escala.setIgrejaId(igrejaId);
        escala.setDataEvento(LocalDate.of(2026, 8, 1));
        escala.setHoraInicio(LocalTime.MIDNIGHT);
        escala.setAtivo(true);
        escala.setStatus(StatusEscala.SUGERIDA);
        escala.setCreatedAt(Instant.now());
        escala.setUpdatedAt(Instant.now());
        return escala;
    }

    private CronogramaEvento eventoDeReferencia() {
        CronogramaEvento evento = new CronogramaEvento();
        evento.setId(UUID.randomUUID());
        evento.setTitulo("Adoração jovem");
        evento.setDataEvento(LocalDate.of(2026, 8, 3));
        evento.setHoraInicio(LocalTime.of(18, 0));
        evento.setHoraFim(LocalTime.of(20, 0));
        evento.setIgrejaId(igrejaId);
        evento.setCompetencia("2026-08");
        evento.setAtivo(true);
        return evento;
    }

    private Culto cultoDomingo() {
        Culto culto = new Culto();
        culto.setId(UUID.randomUUID());
        culto.setTitulo("Culto de domingo");
        culto.setData(LocalDate.of(2026, 8, 2));
        culto.setHoraInicio(LocalTime.of(9, 0));
        culto.setHoraFim(LocalTime.of(10, 30));
        culto.setIgrejaId(igrejaId);
        culto.setStatus(StatusCulto.AGENDADO);
        culto.setAtivo(true);
        culto.setCompetencia("2026-08");
        return culto;
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

    @Test
    void deveRejeitarEscalaDeMinisterioSemMinisterio() {
        assertThatThrownBy(() -> escalaService.criar(new EscalaCreateRequest(
                "Louvor", null, competenciaAberta(), TipoEscala.MINISTERIO,
                igrejaId, null, null, null, null, null, null, null, null, null, List.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ministerioId é obrigatório para escala de ministério");
    }

    @Test
    void deveRejeitarCompetenciaInvalida() {
        assertThatThrownBy(() -> escalaService.criar(new EscalaCreateRequest(
                "Louvor", null, "08-2026", TipoEscala.CULTO,
                igrejaId, null, null, null, null, null, null, null, null, null, List.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Competência inválida");
    }

    @Test
    void deveRejeitarCriacaoAntesDaAberturaDaJanela() {
        assertThatThrownBy(() -> escalaService.criar(new EscalaCreateRequest(
                "Louvor", null, competenciaFechada(), TipoEscala.CULTO,
                igrejaId, null, null, null, null, null, null, null, null, null, List.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Aguarde a abertura da janela");
        verify(escalaRepository, never()).save(any(Escala.class));
    }

    @Test
    void naoDeveCriarEscalaParaIgrejaForaDoAcessoDoUsuario() {
        doThrow(new BusinessException("Sem acesso a esta igreja"))
                .when(autorizacaoService).assertAcesso(igrejaId);

        assertThatThrownBy(() -> escalaService.criar(new EscalaCreateRequest(
                "Louvor", null, competenciaAberta(), TipoEscala.CULTO,
                igrejaId, null, null, null, null, null, null, null, null, null, List.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Sem acesso a esta igreja");
        verify(escalaRepository, never()).save(any(Escala.class));
    }

    @Test
    void deveRejeitarServicoDeMinisterioDeOutraIgreja() {
        Escala escala = escala();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(ministerioRepository.findById(ministerioId)).thenReturn(Optional.of(ministerioDeOutraIgreja()));

        assertThatThrownBy(() -> escalaService.adicionarServico(
                escalaId, new EscalaServicoUpsertRequest(servicoId, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("O ministério não pertence a esta igreja");
        verify(escalaServicoRepository, never()).save(any(EscalaServico.class));
    }

    @Test
    void deveRejeitarHoraFimNaoPosteriorAoInicio() {
        assertThatThrownBy(() -> escalaService.criar(new EscalaCreateRequest(
                "Louvor", null, competenciaAberta(), TipoEscala.CULTO,
                igrejaId, null, null, null, null, LocalTime.of(20, 0), LocalTime.of(19, 0),
                null, null, List.of(), List.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Hora fim deve ser posterior");
        verify(escalaRepository, never()).save(any(Escala.class));
    }

    @Test
    void deveCriarEscalaComSupervisorPregadorEItensPadrao() {
        String competencia = competenciaAberta();

        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja());
        when(pessoaService.buscarEntidade(pastorId)).thenReturn(pastor());
        when(escalaRepository.save(any(Escala.class))).thenAnswer(invocation -> {
            Escala escala = invocation.getArgument(0);
            escala.setId(escalaId);
            escala.setCreatedAt(Instant.now());
            escala.setUpdatedAt(Instant.now());
            return escala;
        });
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(pessoa());
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(ministerioRepository.findById(ministerioId)).thenReturn(Optional.of(ministerio()));
        when(escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, servicoId)).thenReturn(Optional.empty());
        when(escalaServicoRepository.save(any(EscalaServico.class))).thenAnswer(invocation -> {
            EscalaServico escalaServico = invocation.getArgument(0);
            escalaServico.setId(UUID.randomUUID());
            return escalaServico;
        });
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of(escalaServico()));
        when(escalaVoluntarioRepository.save(any(EscalaVoluntario.class))).thenAnswer(invocation -> {
            EscalaVoluntario designacao = invocation.getArgument(0);
            designacao.setId(UUID.randomUUID());
            designacao.setCreatedAt(Instant.now());
            designacao.setUpdatedAt(Instant.now());
            return designacao;
        });
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());

        EscalaResponse response = escalaService.criar(new EscalaCreateRequest(
                "Escala Louvor", "Descricao", competencia, TipoEscala.CULTO,
                igrejaId, null, null, null, null, null, null, "Templo", null, List.of(),
                List.of(new EscalaCreateRequest.EscalaItemRequest(
                        LocalDate.of(2026, 8, 16), null, pessoaId, servicoId, LocalTime.of(18, 0)))));

        assertThat(response.titulo()).isEqualTo("Escala Louvor");
        assertThat(response.supervisorPessoaId()).isEqualTo(pastorId);
        assertThat(response.pregadorPessoaId()).isEqualTo(pastorId);
        assertThat(response.status()).isEqualTo(StatusEscala.EDITANDO);
        assertThat(response.dataEvento()).isEqualTo(YearMonth.parse(competencia).atDay(1));
        assertThat(response.horaInicio()).isEqualTo(LocalTime.MIDNIGHT);

        ArgumentCaptor<EscalaVoluntario> captor = ArgumentCaptor.forClass(EscalaVoluntario.class);
        verify(escalaVoluntarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPessoaId()).isEqualTo(pessoaId);
        assertThat(captor.getValue().getServicoId()).isEqualTo(servicoId);
        assertThat(captor.getValue().getDataItem()).isEqualTo(LocalDate.of(2026, 8, 16));
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusEscalaVoluntario.PENDENTE);
    }

    @Test
    void naoDeveListarEscalasQuandoNaoHaIgrejasNoEscopo() {
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of());

        assertThat(escalaService.listar(igrejaId, null, null)).isEmpty();
        verify(escalaRepository, never()).findByIgrejaIdInOrderByCompetenciaDescTituloAsc(any());
    }

    @Test
    void naoDeveListarEscalasDeOutraPessoaParaVoluntarioComum() {
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(autorizacaoService.requireUsuario()).thenReturn(usuarioLogado(pessoaId));
        when(autorizacaoService.isPastorOuAdmin(igrejaId)).thenReturn(false);

        assertThatThrownBy(() -> escalaService.listar(igrejaId, null, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Sem permissão para ver escalas de outra pessoa");
    }

    @Test
    void liderDeveListarEscalasDeOutroVoluntario() {
        UUID alvoId = UUID.randomUUID();
        Escala escalaVisivel = escala();

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(autorizacaoService.requireUsuario()).thenReturn(usuarioLogado(pessoaId));
        when(autorizacaoService.isPastorOuAdmin(igrejaId)).thenReturn(true);
        when(escalaRepository.findByIgrejaIdInAndCompetenciaOrderByTipoAscTituloAsc(List.of(igrejaId), "2026-08"))
                .thenReturn(List.of(escalaVisivel));
        when(autorizacaoService.podeVerEscala(escalaVisivel)).thenReturn(true);
        when(escalaRepository.findByIgrejaIdInAndPessoaDesignada(List.of(igrejaId), alvoId))
                .thenReturn(List.of(escalaVisivel));
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());

        List<EscalaResponse> escalas = escalaService.listar(igrejaId, "2026-08", alvoId);

        assertThat(escalas).hasSize(1);
        verify(escalaRepository, never()).findByIgrejaIdInOrderByCompetenciaDescTituloAsc(any());
    }

    @Test
    void naoDeveBuscarEscalaQueUsuarioNaoPodeVer() {
        Escala escala = escala();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(autorizacaoService.podeVerEscala(escala)).thenReturn(false);

        assertThatThrownBy(() -> escalaService.buscarPorId(escalaId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Sem permissão para ver esta escala");
    }

    @Test
    void deveAtualizarEscalaMantendoCamposNaoInformados() {
        Escala escala = escala();
        escala.setSupervisorPessoaId(pastorId);

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaRepository.save(any(Escala.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(pessoaService.buscarEntidade(pastorId)).thenReturn(pastor());

        EscalaResponse response = escalaService.atualizar(escalaId, new EscalaUpdateRequest(
                "Escala Culto Domingo", "Nova descricao", null, null, null,
                null, null, null, null, null, "Novo local", null, true));

        verify(autorizacaoService, times(2))
                .assertPodeMutarEscala(TipoEscala.CULTO, igrejaId, "2026-08", null);
        assertThat(response.titulo()).isEqualTo("Escala Culto Domingo");
        assertThat(response.localEvento()).isEqualTo("Novo local");
        assertThat(response.tipo()).isEqualTo(TipoEscala.CULTO);
        assertThat(response.supervisorPessoaId()).isEqualTo(pastorId);
    }

    @Test
    void deveRejeitarMudancaParaMinisterioSemMinisterioNaAtualizacao() {
        Escala escala = escala();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));

        assertThatThrownBy(() -> escalaService.atualizar(escalaId, new EscalaUpdateRequest(
                "Escala", null, null, TipoEscala.MINISTERIO, null,
                null, null, null, null, null, null, null, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ministerioId é obrigatório para escala de ministério");
        verify(escalaRepository, never()).save(any(Escala.class));
    }

    @Test
    void deveRejeitarCompetenciaFechadaNaAtualizacao() {
        Escala escala = escala();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));

        assertThatThrownBy(() -> escalaService.atualizar(escalaId, new EscalaUpdateRequest(
                "Escala", null, competenciaFechada(), null, null,
                null, null, null, null, null, null, null, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Aguarde a abertura da janela");
        verify(escalaRepository, never()).save(any(Escala.class));
    }

    @Test
    void deveFinalizarEscalaRegistrandoDataDeFinalizacao() {
        Escala escala = escala();
        escala.setStatus(StatusEscala.EM_ANDAMENTO);
        escala.setDataLancamento(Instant.now());

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaRepository.save(any(Escala.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());

        EscalaResponse response = escalaService.atualizarStatus(
                escalaId, new EscalaStatusRequest(StatusEscala.FINALIZADA));

        assertThat(response.status()).isEqualTo(StatusEscala.FINALIZADA);
        assertThat(response.dataFinalizacao()).isNotNull();
        assertThat(response.dataLancamento()).isNull();
    }

    @Test
    void devePermitirVoltarDeEmAndamentoParaEditandoLimpandoLancamento() {
        Escala escala = escala();
        escala.setStatus(StatusEscala.EM_ANDAMENTO);
        escala.setDataLancamento(Instant.now());

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaRepository.save(any(Escala.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());

        EscalaResponse response = escalaService.atualizarStatus(
                escalaId, new EscalaStatusRequest(StatusEscala.EDITANDO));

        assertThat(response.status()).isEqualTo(StatusEscala.EDITANDO);
        assertThat(response.dataLancamento()).isNull();
    }

    @Test
    void deveAceitarManterOStatusJaRegistrado() {
        Escala escala = escala();
        escala.setStatus(StatusEscala.FINALIZADA);
        escala.setDataFinalizacao(Instant.now());

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaRepository.save(any(Escala.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());

        EscalaResponse response = escalaService.atualizarStatus(
                escalaId, new EscalaStatusRequest(StatusEscala.FINALIZADA));

        assertThat(response.status()).isEqualTo(StatusEscala.FINALIZADA);
        assertThat(response.dataFinalizacao()).isNotNull();
    }

    @Test
    void deveAdicionarServicoPrevistoNaEscala() {
        Escala escala = escala();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(ministerioRepository.findById(ministerioId)).thenReturn(Optional.of(ministerio()));
        when(escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, servicoId)).thenReturn(Optional.empty());
        when(escalaServicoRepository.save(any(EscalaServico.class))).thenAnswer(invocation -> {
            EscalaServico escalaServico = invocation.getArgument(0);
            escalaServico.setId(UUID.randomUUID());
            return escalaServico;
        });

        EscalaResponse.EscalaServicoResponse response = escalaService.adicionarServico(
                escalaId, new EscalaServicoUpsertRequest(servicoId, 2));

        assertThat(response.servicoNome()).isEqualTo("Violão");
        assertThat(response.quantidadeNecessaria()).isEqualTo(2);
    }

    @Test
    void deveRejeitarServicoJaPresenteNaEscala() {
        Escala escala = escala();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(ministerioRepository.findById(ministerioId)).thenReturn(Optional.of(ministerio()));
        when(escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, servicoId))
                .thenReturn(Optional.of(escalaServico()));

        assertThatThrownBy(() -> escalaService.adicionarServico(
                escalaId, new EscalaServicoUpsertRequest(servicoId, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Este serviço já está na escala");
        verify(escalaServicoRepository, never()).save(any(EscalaServico.class));
    }

    @Test
    void naoDeveAtualizarServicoDeOutraEscala() {
        Escala escala = escala();
        EscalaServico outro = escalaServico();
        outro.setEscalaId(UUID.randomUUID());

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaServicoRepository.findById(outro.getId())).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> escalaService.atualizarServico(
                escalaId, outro.getId(), new EscalaServicoUpsertRequest(servicoId, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("O serviço não pertence a esta escala");
        verify(escalaServicoRepository, never()).save(any(EscalaServico.class));
    }

    @Test
    void naoDeveRemoverServicoDeOutraEscala() {
        Escala escala = escala();
        EscalaServico outro = escalaServico();
        outro.setEscalaId(UUID.randomUUID());

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaServicoRepository.findById(outro.getId())).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> escalaService.removerServico(escalaId, outro.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("O serviço não pertence a esta escala");
        verify(escalaServicoRepository, never()).delete(any(EscalaServico.class));
    }

    @Test
    void deveRejeitarDesignacaoDeServicoNaoPrevistoNaEscala() {
        Escala escala = escala();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(pessoa());
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, servicoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> escalaService.designarVoluntario(
                escalaId, new EscalaVoluntarioCreateRequest(pessoaId, servicoId, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Este serviço não está previsto nesta escala");
        verify(escalaVoluntarioRepository, never()).save(any(EscalaVoluntario.class));
    }

    @Test
    void deveRejeitarDuplicidadeDeDesignacaoNaMesmaData() {
        Escala escala = escala();
        LocalDate data = LocalDate.of(2026, 8, 24);
        PessoaServico vinculo = new PessoaServico();
        vinculo.setAtivo(true);

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(pessoa());
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, servicoId))
                .thenReturn(Optional.of(escalaServico()));
        when(pessoaServicoRepository.findByPessoaIdAndServicoId(pessoaId, servicoId))
                .thenReturn(Optional.of(vinculo));
        when(escalaVoluntarioRepository.findByEscalaIdAndPessoaIdAndServicoIdAndDataItem(
                escalaId, pessoaId, servicoId, data)).thenReturn(Optional.of(designacao()));

        assertThatThrownBy(() -> escalaService.designarVoluntario(
                escalaId, new EscalaVoluntarioCreateRequest(pessoaId, servicoId, null, data, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("A pessoa já está designada para este serviço nesta data");
        verify(escalaVoluntarioRepository, never()).save(any(EscalaVoluntario.class));
    }

    @Test
    void deveRejeitarDesignacaoQuandoQuantidadeNecessariaJaFoiPreenchida() {
        Escala escala = escala();
        LocalDate data = LocalDate.of(2026, 8, 24);
        PessoaServico vinculo = new PessoaServico();
        vinculo.setAtivo(true);

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(pessoa());
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, servicoId))
                .thenReturn(Optional.of(escalaServico()));
        when(pessoaServicoRepository.findByPessoaIdAndServicoId(pessoaId, servicoId))
                .thenReturn(Optional.of(vinculo));
        when(escalaVoluntarioRepository.findByEscalaIdAndPessoaIdAndServicoIdAndDataItem(
                escalaId, pessoaId, servicoId, data)).thenReturn(Optional.empty());
        when(escalaVoluntarioRepository.countByEscalaIdAndServicoIdAndDataItemAndStatusIn(
                eq(escalaId), eq(servicoId), eq(data), anyList())).thenReturn(1L);

        assertThatThrownBy(() -> escalaService.designarVoluntario(
                escalaId, new EscalaVoluntarioCreateRequest(pessoaId, servicoId, null, data, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quantidade necessária para este serviço já foi preenchida");
        verify(escalaVoluntarioRepository, never()).save(any(EscalaVoluntario.class));
    }

    @Test
    void naoDeveDesignarParaEventoDeOutraIgreja() {
        Escala escala = escala();
        CronogramaEvento evento = new CronogramaEvento();
        evento.setId(UUID.randomUUID());
        evento.setIgrejaId(UUID.randomUUID());
        evento.setDataEvento(LocalDate.of(2026, 8, 24));
        evento.setHoraInicio(LocalTime.of(18, 0));
        PessoaServico vinculo = new PessoaServico();
        vinculo.setAtivo(true);

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(pessoa());
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, servicoId))
                .thenReturn(Optional.of(escalaServico()));
        when(pessoaServicoRepository.findByPessoaIdAndServicoId(pessoaId, servicoId))
                .thenReturn(Optional.of(vinculo));
        when(cronogramaService.buscarEntidade(evento.getId())).thenReturn(evento);

        assertThatThrownBy(() -> escalaService.designarVoluntario(
                escalaId, new EscalaVoluntarioCreateRequest(pessoaId, servicoId, null, null, evento.getId(), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("O evento não pertence a esta igreja");
        verify(escalaVoluntarioRepository, never()).save(any(EscalaVoluntario.class));
    }

    @Test
    void naoDeveDesignarPessoaDeOutraIgreja() {
        Escala escala = escala();
        Pessoa alheia = pessoa();
        alheia.setIgrejaId(UUID.randomUUID());

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(alheia);
        doThrow(new BusinessException("A pessoa não pertence a esta igreja"))
                .when(autorizacaoService).assertPessoaDaIgreja(alheia, igrejaId);

        assertThatThrownBy(() -> escalaService.designarVoluntario(
                escalaId, new EscalaVoluntarioCreateRequest(pessoaId, servicoId, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pertence a esta igreja");
        verify(escalaVoluntarioRepository, never()).save(any(EscalaVoluntario.class));
    }

    @Test
    void deveTrocarPessoaEServicoDaDesignacao() {
        Escala escala = escala();
        EscalaVoluntario designacao = designacao();
        UUID novaPessoaId = UUID.randomUUID();
        Pessoa novaPessoa = pessoa();
        novaPessoa.setId(novaPessoaId);
        novaPessoa.setNome("Bruno");
        PessoaServico vinculo = new PessoaServico();
        vinculo.setAtivo(true);

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaVoluntarioRepository.findById(designacao.getId())).thenReturn(Optional.of(designacao));
        when(pessoaService.buscarEntidade(novaPessoaId)).thenReturn(novaPessoa);
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, servicoId))
                .thenReturn(Optional.of(escalaServico()));
        when(pessoaServicoRepository.findByPessoaIdAndServicoId(novaPessoaId, servicoId))
                .thenReturn(Optional.of(vinculo));
        when(escalaVoluntarioRepository.findByEscalaIdAndPessoaIdAndServicoId(
                escalaId, novaPessoaId, servicoId)).thenReturn(List.of());
        when(escalaVoluntarioRepository.save(any(EscalaVoluntario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EscalaResponse.EscalaVoluntarioResponse response = escalaService.atualizarDesignacao(
                escalaId,
                designacao.getId(),
                new EscalaVoluntarioUpdateRequest(novaPessoaId, servicoId, StatusEscalaVoluntario.RECUSADO, "Troca")
        );

        assertThat(response.pessoaId()).isEqualTo(novaPessoaId);
        assertThat(response.pessoaNome()).isEqualTo("Bruno");
        assertThat(response.status()).isEqualTo(StatusEscalaVoluntario.RECUSADO);
        assertThat(response.observacoes()).isEqualTo("Troca");
    }

    @Test
    void naoDeveTrocarParaPessoaJaDesignadaNoMesmoServico() {
        Escala escala = escala();
        EscalaVoluntario designacao = designacao();
        UUID novaPessoaId = UUID.randomUUID();
        Pessoa novaPessoa = pessoa();
        novaPessoa.setId(novaPessoaId);
        PessoaServico vinculo = new PessoaServico();
        vinculo.setAtivo(true);
        EscalaVoluntario outra = designacao();
        outra.setId(UUID.randomUUID());
        outra.setPessoaId(novaPessoaId);
        outra.setDataItem(LocalDate.of(2026, 9, 6));
        EscalaVoluntario outraEmOutraData = designacao();
        outraEmOutraData.setId(UUID.randomUUID());
        outraEmOutraData.setPessoaId(novaPessoaId);
        outraEmOutraData.setDataItem(LocalDate.of(2026, 9, 13));

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaVoluntarioRepository.findById(designacao.getId())).thenReturn(Optional.of(designacao));
        when(pessoaService.buscarEntidade(novaPessoaId)).thenReturn(novaPessoa);
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(escalaServicoRepository.findByEscalaIdAndServicoId(escalaId, servicoId))
                .thenReturn(Optional.of(escalaServico()));
        when(pessoaServicoRepository.findByPessoaIdAndServicoId(novaPessoaId, servicoId))
                .thenReturn(Optional.of(vinculo));
        when(escalaVoluntarioRepository.findByEscalaIdAndPessoaIdAndServicoId(
                escalaId, novaPessoaId, servicoId)).thenReturn(List.of(outra, outraEmOutraData));

        assertThatThrownBy(() -> escalaService.atualizarDesignacao(
                escalaId,
                designacao.getId(),
                new EscalaVoluntarioUpdateRequest(novaPessoaId, servicoId, StatusEscalaVoluntario.CONFIRMADO, null)
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("A pessoa já está designada para este serviço nesta escala");
        verify(escalaVoluntarioRepository, never()).save(any(EscalaVoluntario.class));
    }

    @Test
    void naoDeveRemoverDesignacaoDeOutraEscala() {
        Escala escala = escala();
        EscalaVoluntario designacao = designacao();
        designacao.setEscalaId(UUID.randomUUID());

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaVoluntarioRepository.findById(designacao.getId())).thenReturn(Optional.of(designacao));

        assertThatThrownBy(() -> escalaService.removerDesignacao(escalaId, designacao.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("A designação não pertence a esta escala");
        verify(escalaVoluntarioRepository, never()).delete(any(EscalaVoluntario.class));
    }

    @Test
    void deveRemoverDesignacaoDaMesmaEscala() {
        Escala escala = escala();
        EscalaVoluntario designacao = designacao();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaVoluntarioRepository.findById(designacao.getId())).thenReturn(Optional.of(designacao));

        escalaService.removerDesignacao(escalaId, designacao.getId());

        verify(escalaVoluntarioRepository).delete(designacao);
    }

    @Test
    void naoDeveExcluirEscalaQueUsuarioNaoPodeMutar() {
        Escala escala = escala();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        doThrow(new BusinessException("Escala já lançada não pode ser alterada"))
                .when(autorizacaoService)
                .assertPodeMutarEscala(TipoEscala.CULTO, igrejaId, "2026-08", null);

        assertThatThrownBy(() -> escalaService.excluir(escalaId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Escala já lançada não pode ser alterada");
        verify(escalaRepository, never()).delete(any(Escala.class));
    }

    @Test
    void deveExcluirEscalaPermitida() {
        Escala escala = escala();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));

        escalaService.excluir(escalaId);

        verify(escalaRepository).delete(escala);
    }

    @Test
    void naoDeveResponderDesignacaoComStatusPendente() {
        Escala escala = escala();
        escala.setStatus(StatusEscala.EM_ANDAMENTO);
        EscalaVoluntario designacao = designacao();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaVoluntarioRepository.findById(designacao.getId())).thenReturn(Optional.of(designacao));
        when(autorizacaoService.requireUsuario()).thenReturn(usuarioLogado(pessoaId));

        assertThatThrownBy(() -> escalaService.responder(
                escalaId,
                designacao.getId(),
                new EscalaVoluntarioRespostaRequest(StatusEscalaVoluntario.PENDENTE, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("O voluntário só pode confirmar ou recusar a designação");
    }

    @Test
    void deveRegistrarRecusaComMotivo() {
        Escala escala = escala();
        escala.setStatus(StatusEscala.EM_ANDAMENTO);
        EscalaVoluntario designacao = designacao();

        when(escalaRepository.findById(escalaId)).thenReturn(Optional.of(escala));
        when(escalaVoluntarioRepository.findById(designacao.getId())).thenReturn(Optional.of(designacao));
        when(autorizacaoService.requireUsuario()).thenReturn(usuarioLogado(pessoaId));
        when(pessoaService.buscarEntidade(pessoaId)).thenReturn(pessoa());
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());
        when(escalaVoluntarioRepository.save(any(EscalaVoluntario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EscalaResponse.EscalaVoluntarioResponse response = escalaService.responder(
                escalaId,
                designacao.getId(),
                new EscalaVoluntarioRespostaRequest(StatusEscalaVoluntario.RECUSADO, "Choque de datas")
        );

        assertThat(response.status()).isEqualTo(StatusEscalaVoluntario.RECUSADO);
        assertThat(response.confirmadoPeloVoluntario()).isFalse();
        assertThat(response.observacoes()).isEqualTo("Choque de datas");
    }

    @Test
    void naoDeveExibirVisaoMensalDeMinisterioForaDoEscopo() {
        Ministerio alheio = ministerio();
        alheio.setNome("Teatro");

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(autorizacaoService.requireUsuario()).thenReturn(usuarioLogado(pessoaId));
        when(ministerioRepository.findByIgrejaIdInOrderByNomeAsc(anyList())).thenReturn(List.of(alheio));

        assertThatThrownBy(() -> escalaService.visaoMensal(igrejaId, "2026-08", UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Ministério não encontrado ou fora do seu acesso");
    }

    @Test
    void naoDeveExibirVisaoMensalSemMinisterioLiderado() {
        Ministerio alheio = ministerio();
        alheio.setNome("Teatro");
        alheio.setLiderPessoaId(UUID.randomUUID());

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(autorizacaoService.requireUsuario()).thenReturn(usuarioLogado(pessoaId));
        when(autorizacaoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(ministerioRepository.findByIgrejaIdInOrderByNomeAsc(anyList())).thenReturn(List.of(alheio));

        assertThatThrownBy(() -> escalaService.visaoMensal(igrejaId, "2026-08", null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("informe um ministerioId de um ministério que você lidera");
    }

    @Test
    void coordenadorDeveVerAVisaoMensalDoSeuMinisterio() {
        Ministerio departamento = ministerio();
        departamento.setCoordenadorPessoaId(pessoaId);

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(autorizacaoService.requireUsuario()).thenReturn(usuarioLogado(pessoaId));
        when(autorizacaoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(ministerioRepository.findByIgrejaIdInOrderByNomeAsc(anyList())).thenReturn(List.of(departamento));
        when(escalaRepository.findByMinisterioIdAndCompetenciaAndAtivoTrueOrderByDataEventoAsc(ministerioId, "2026-08"))
                .thenReturn(List.of());
        when(cultoRepository.filtrar(anyList(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(cronogramaEventoRepository
                .findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(anyList(), any(), any()))
                .thenReturn(List.of());

        List<VisaoMensalEscalaResponse> visoes = escalaService.visaoMensal(igrejaId, "2026-08", null);

        assertThat(visoes).singleElement().satisfies(visao -> {
            assertThat(visao.ministerioId()).isEqualTo(ministerioId);
            assertThat(visao.escalaId()).isNull();
            assertThat(visao.escalaStatus()).isNull();
            assertThat(visao.itens()).isEmpty();
        });
    }

    @Test
    void deveRejeitarCompetenciaInvalidaNaVisaoMensal() {
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));

        assertThatThrownBy(() -> escalaService.visaoMensal(igrejaId, "08-2026", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Competência inválida");
    }

    @Test
    void deveOcultarEventosInativosNaVisaoMensal() {
        Ministerio ministerioLiderado = ministerio();
        ministerioLiderado.setLiderPessoaId(pessoaId);
        CronogramaEvento ativo = eventoDeReferencia();
        CronogramaEvento inativo = eventoDeReferencia();
        inativo.setAtivo(false);

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(autorizacaoService.requireUsuario()).thenReturn(usuarioLogado(pessoaId));
        when(autorizacaoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(ministerioRepository.findByIgrejaIdInOrderByNomeAsc(anyList())).thenReturn(List.of(ministerioLiderado));
        when(escalaRepository.findByMinisterioIdAndCompetenciaAndAtivoTrueOrderByDataEventoAsc(ministerioId, "2026-08"))
                .thenReturn(List.of(escalaMensal()));
        when(escalaServicoRepository.findByEscalaId(escalaId)).thenReturn(List.of(escalaServico()));
        when(escalaVoluntarioRepository.findByEscalaId(escalaId)).thenReturn(List.of());
        when(cultoRepository.filtrar(anyList(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(cronogramaEventoRepository
                .findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(anyList(), any(), any()))
                .thenReturn(List.of(ativo, inativo));
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico());

        List<VisaoMensalEscalaResponse> visoes = escalaService.visaoMensal(igrejaId, "2026-08", null);

        assertThat(visoes).singleElement().satisfies(visao -> {
            assertThat(visao.itens()).hasSize(1);
            assertThat(visao.itens().get(0).origem()).isEqualTo("EVENTO");
            assertThat(visao.itens().get(0).titulo()).isEqualTo("Adoração jovem");
        });
    }

    private String competenciaAberta() {
        return YearMonth.now().minusMonths(1).toString();
    }

    private String competenciaFechada() {
        return YearMonth.now().plusMonths(3).toString();
    }

    private Igreja igreja() {
        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setPastorPessoaId(pastorId);
        return igreja;
    }

    private Ministerio ministerioDeOutraIgreja() {
        Ministerio ministerio = new Ministerio();
        ministerio.setId(ministerioId);
        ministerio.setNome("Teatro");
        ministerio.setIgrejaId(UUID.randomUUID());
        return ministerio;
    }
}

