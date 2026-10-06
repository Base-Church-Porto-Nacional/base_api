package com.example.base_api.culto.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.culto.dto.CultoConteudoResponse;
import com.example.base_api.culto.dto.CultoConteudoUpdateRequest;
import com.example.base_api.culto.dto.CultoCreateRequest;
import com.example.base_api.culto.dto.CultoRelatorioConsolidadoResponse;
import com.example.base_api.culto.dto.CultoRelatorioRequest;
import com.example.base_api.culto.dto.CultoRelatorioResponse;
import com.example.base_api.culto.dto.CultoResponse;
import com.example.base_api.culto.dto.CultoUpdateRequest;
import com.example.base_api.culto.entity.CategoriaCulto;
import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.entity.CultoRelatorio;
import com.example.base_api.culto.entity.DiaSemana;
import com.example.base_api.culto.entity.StatusCulto;
import com.example.base_api.culto.repository.CategoriaCultoRepository;
import com.example.base_api.culto.repository.CultoRelatorioRepository;
import com.example.base_api.culto.repository.CultoRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CultoServiceTest {

    @Mock
    private CultoRepository cultoRepository;
    @Mock
    private CategoriaCultoRepository categoriaRepository;
    @Mock
    private CultoRelatorioRepository relatorioRepository;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private PessoaRepository pessoaRepository;

    @InjectMocks
    private CultoService cultoService;

    private CategoriaCulto categoria(UUID igrejaId) {
        CategoriaCulto categoria = new CategoriaCulto();
        categoria.setId(UUID.randomUUID());
        categoria.setIgrejaId(igrejaId);
        categoria.setTitulo("Domingo inesquecível");
        categoria.setDiaSemana(DiaSemana.DOMINGO);
        categoria.setHoraInicio(LocalTime.of(20, 0));
        categoria.setDuracaoMinutos(90);
        return categoria;
    }

    private Culto culto(UUID igrejaId, StatusCulto status) {
        Culto culto = new Culto();
        culto.setId(UUID.randomUUID());
        culto.setIgrejaId(igrejaId);
        culto.setCategoriaCultoId(UUID.randomUUID());
        culto.setData(LocalDate.of(2026, 8, 16));
        culto.setHoraInicio(LocalTime.of(20, 0));
        culto.setStatus(status);
        return culto;
    }

    @Test
    void deveCriarCultoHerdandoPadroesDaCategoria() {
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = categoria(igrejaId);
        LocalDate data = LocalDate.of(2026, 8, 16);

        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        when(cultoRepository.findByCategoriaCultoIdAndData(categoria.getId(), data)).thenReturn(Optional.empty());
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> {
            Culto c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });

        CultoResponse response = cultoService.criar(new CultoCreateRequest(
                categoria.getId(), igrejaId, null, null, data, null, null, null
        ));

        assertThat(response.titulo()).isEqualTo("Domingo inesquecível");
        assertThat(response.horaInicio()).isEqualTo(LocalTime.of(20, 0));
        assertThat(response.horaFim()).isEqualTo(LocalTime.of(21, 30));
        assertThat(response.status()).isEqualTo(StatusCulto.AGENDADO);
        assertThat(response.competencia()).isEqualTo("2026-08");
    }

    @Test
    void deveRejeitarCultoDuplicadoNaMesmaData() {
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = categoria(igrejaId);
        LocalDate data = LocalDate.of(2026, 8, 16);

        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        when(cultoRepository.findByCategoriaCultoIdAndData(categoria.getId(), data))
                .thenReturn(Optional.of(culto(igrejaId, StatusCulto.AGENDADO)));

        assertThatThrownBy(() -> cultoService.criar(new CultoCreateRequest(
                categoria.getId(), igrejaId, null, null, data, null, null, null
        ))).isInstanceOf(ConflictException.class);
    }

    @Test
    void deveBloquearTransicaoDiretaDeAgendadoParaRealizado() {
        UUID igrejaId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.AGENDADO);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));

        assertThatThrownBy(() -> cultoService.atualizar(culto.getId(), new CultoUpdateRequest(
                "Domingo inesquecível", null, culto.getData(), LocalTime.of(20, 0), null, null, StatusCulto.REALIZADO
        ))).isInstanceOf(BusinessException.class);
    }

    @Test
    void devePermitirAbrirCultoParaConfiguracao() {
        UUID igrejaId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.AGENDADO);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));
        when(relatorioRepository.existsByCultoId(culto.getId())).thenReturn(false);
        when(categoriaRepository.findById(any())).thenReturn(Optional.of(categoria(igrejaId)));
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> {
            Culto c = invocation.getArgument(0);
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });

        CultoResponse response = cultoService.atualizar(culto.getId(), new CultoUpdateRequest(
                "Domingo inesquecível", "Destravados: Conforto", culto.getData(),
                LocalTime.of(20, 0), null, null, StatusCulto.ABERTO
        ));

        assertThat(response.status()).isEqualTo(StatusCulto.ABERTO);
        assertThat(response.tema()).isEqualTo("Destravados: Conforto");
    }

    @Test
    void deveSalvarConteudoComPermissoesDePregadorELouvor() {
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.ABERTO);
        culto.setPregadorId(pregadorId);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));
        when(autorizacaoService.podeEditarTexto(igrejaId, pregadorId)).thenReturn(true);
        when(autorizacaoService.podeEditarLouvor(igrejaId)).thenReturn(true);
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> {
            Culto c = invocation.getArgument(0);
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });

        CultoConteudoResponse response = cultoService.salvarConteudo(culto.getId(),
                new CultoConteudoUpdateRequest("João 15", "Bondade de Deus", null, null));

        assertThat(response.textoPregacao()).isEqualTo("João 15");
        assertThat(response.louvor()).isEqualTo("Bondade de Deus");
    }

    @Test
    void deveBloquearSalvarConteudoSemPermissao() {
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.ABERTO);
        culto.setPregadorId(pregadorId);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));
        when(autorizacaoService.podeEditarTexto(igrejaId, pregadorId)).thenReturn(false);
        when(autorizacaoService.podeEditarLouvor(igrejaId)).thenReturn(false);

        assertThatThrownBy(() -> cultoService.salvarConteudo(culto.getId(),
                new CultoConteudoUpdateRequest("texto spoof", null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deveSalvarRelatorioConsolidarTotaisEMarcarRealizado() {
        UUID igrejaId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.ABERTO);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));
        when(relatorioRepository.findByCultoId(culto.getId())).thenReturn(Optional.empty());
        when(relatorioRepository.save(any())).thenAnswer(invocation -> {
            com.example.base_api.culto.entity.CultoRelatorio r = invocation.getArgument(0);
            r.setCreatedAt(Instant.now());
            r.setUpdatedAt(Instant.now());
            return r;
        });
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> {
            Culto c = invocation.getArgument(0);
            c.setUpdatedAt(Instant.now());
            return c;
        });

        CultoRelatorioResponse response = cultoService.salvarRelatorio(culto.getId(),
                new CultoRelatorioRequest(19, 2, 2, 1, 3, 7, 4, 0, "Celebração"));

        assertThat(response.totalPresentes()).isEqualTo(21);
        assertThat(response.totalVeiculos()).isEqualTo(11);
        assertThat(response.carros()).isEqualTo(7);

        ArgumentCaptor<Culto> captor = ArgumentCaptor.forClass(Culto.class);
        verify(cultoRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusCulto.REALIZADO);
    }

    @Test
    void deveBloquearExcluirCultoComRelatorio() {
        UUID igrejaId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.REALIZADO);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));
        when(relatorioRepository.existsByCultoId(culto.getId())).thenReturn(true);

        assertThatThrownBy(() -> cultoService.excluir(culto.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveListarCultosIndicandoJanelaDeConfiguracao() {
        UUID igrejaId = UUID.randomUUID();
        Culto proximo = culto(igrejaId, StatusCulto.AGENDADO);
        proximo.setData(LocalDate.now().plusDays(3));
        Culto distante = culto(igrejaId, StatusCulto.AGENDADO);
        distante.setData(LocalDate.now().plusDays(30));

        when(autorizacaoService.idsParaListagem(null)).thenReturn(List.of(igrejaId));
        when(cultoRepository.filtrar(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(proximo, distante));
        when(relatorioRepository.findByCultoIdIn(any())).thenReturn(List.of());
        when(categoriaRepository.findAllById(any())).thenReturn(List.of(categoria(igrejaId)));

        List<CultoResponse> cultos = cultoService.listar(null, null, null, null, null, null, null);

        assertThat(cultos).hasSize(2);
        assertThat(cultos.get(0).janelaConfiguracaoAberta()).isTrue();
        assertThat(cultos.get(1).janelaConfiguracaoAberta()).isFalse();
    }

    @Test
    void deveCriarCultoRespeitandoTituloEHorariosInformados() {
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = categoria(igrejaId);
        LocalDate data = LocalDate.of(2026, 8, 16);

        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        when(cultoRepository.findByCategoriaCultoIdAndData(categoria.getId(), data)).thenReturn(Optional.empty());
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> {
            Culto c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });

        CultoResponse response = cultoService.criar(new CultoCreateRequest(
                categoria.getId(), igrejaId, " Culto especial ", "Tema do mês",
                data, LocalTime.of(19, 0), LocalTime.of(21, 0), null));

        assertThat(response.titulo()).isEqualTo("Culto especial");
        assertThat(response.tema()).isEqualTo("Tema do mês");
        assertThat(response.horaInicio()).isEqualTo(LocalTime.of(19, 0));
        assertThat(response.horaFim()).isEqualTo(LocalTime.of(21, 0));
        assertThat(response.competencia()).isEqualTo("2026-08");
        assertThat(response.temRelatorio()).isFalse();
    }

    @Test
    void deveRejeitarCriacaoComCategoriaInexistente() {
        assertThatThrownBy(() -> cultoService.criar(new CultoCreateRequest(
                UUID.randomUUID(), UUID.randomUUID(), null, null,
                LocalDate.of(2026, 8, 16), null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Categoria de culto não encontrada");
    }

    @Test
    void naoDeveCriarCultoParaCategoriaDeIgrejaForaDoAcesso() {
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = categoria(igrejaId);

        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        doThrow(new BusinessException("Sem permissão para criar cultos nesta igreja"))
                .when(autorizacaoService).assertPastorOuAdmin(igrejaId);

        assertThatThrownBy(() -> cultoService.criar(new CultoCreateRequest(
                categoria.getId(), igrejaId, null, null,
                LocalDate.of(2026, 8, 16), null, null, null)))
                .isInstanceOf(BusinessException.class);
        verify(cultoRepository, never()).save(any(Culto.class));
    }

    @Test
    void deveRejeitarPregadorDeOutraIgrejaNaCriacao() {
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorId = UUID.randomUUID();
        CategoriaCulto categoria = categoria(igrejaId);
        Pessoa alheia = new Pessoa();
        alheia.setId(pregadorId);
        alheia.setIgrejaId(UUID.randomUUID());

        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        when(pessoaRepository.findById(pregadorId)).thenReturn(Optional.of(alheia));
        doThrow(new BusinessException("A pessoa não pertence a esta igreja"))
                .when(autorizacaoService).assertPessoaDaIgreja(alheia, igrejaId);

        assertThatThrownBy(() -> cultoService.criar(new CultoCreateRequest(
                categoria.getId(), igrejaId, null, null,
                LocalDate.of(2026, 8, 16), null, null, pregadorId)))
                .isInstanceOf(BusinessException.class);
        verify(cultoRepository, never()).save(any(Culto.class));
    }

    @Test
    void deveRejeitarCriacaoComHoraFimNaoPosteriorAoInicio() {
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = categoria(igrejaId);

        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        when(cultoRepository.findByCategoriaCultoIdAndData(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cultoService.criar(new CultoCreateRequest(
                categoria.getId(), igrejaId, null, null,
                LocalDate.of(2026, 8, 16), LocalTime.of(20, 0), LocalTime.of(20, 0), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Hora fim deve ser posterior");
    }

    @Test
    void deveRejeitarMovimentoDeCultoParaDataOcupadaPorOutroCulto() {
        UUID igrejaId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.AGENDADO);
        Culto outro = culto(igrejaId, StatusCulto.AGENDADO);
        outro.setCategoriaCultoId(culto.getCategoriaCultoId());

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));
        when(cultoRepository.findByCategoriaCultoIdAndData(culto.getCategoriaCultoId(), LocalDate.of(2026, 8, 23)))
                .thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> cultoService.atualizar(culto.getId(), new CultoUpdateRequest(
                "Domingo inesquecível", null, LocalDate.of(2026, 8, 23),
                LocalTime.of(20, 0), null, null, StatusCulto.AGENDADO)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Já existe um culto desta categoria na data informada");
    }

    @Test
    void devePermitirManterCultoNaMesmaDataNaAtualizacao() {
        UUID igrejaId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.AGENDADO);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));
        when(relatorioRepository.existsByCultoId(culto.getId())).thenReturn(false);
        when(categoriaRepository.findById(any())).thenReturn(Optional.of(categoria(igrejaId)));
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CultoResponse response = cultoService.atualizar(culto.getId(), new CultoUpdateRequest(
                "Novo título", "Novo tema", culto.getData(),
                LocalTime.of(19, 30), LocalTime.of(21, 0), null, StatusCulto.AGENDADO));

        assertThat(response.titulo()).isEqualTo("Novo título");
        assertThat(response.horaInicio()).isEqualTo(LocalTime.of(19, 30));
        verify(cultoRepository).save(any(Culto.class));
    }

    @Test
    void naoDeveReabrirCultoCancelado() {
        UUID igrejaId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.CANCELADO);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));

        assertThatThrownBy(() -> cultoService.atualizar(culto.getId(), new CultoUpdateRequest(
                "Domingo inesquecível", null, culto.getData(),
                LocalTime.of(20, 0), null, null, StatusCulto.AGENDADO)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");
        verify(cultoRepository, never()).save(any(Culto.class));
    }

    @Test
    void naoDeveReabrirCultoRealizado() {
        UUID igrejaId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.REALIZADO);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));

        assertThatThrownBy(() -> cultoService.atualizar(culto.getId(), new CultoUpdateRequest(
                "Domingo inesquecível", null, culto.getData(),
                LocalTime.of(20, 0), null, null, StatusCulto.ABERTO)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");
    }

    @Test
    void deveRecusarConsultaDeCultoForaDoAcesso() {
        UUID id = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.ABERTO);

        when(cultoRepository.findById(id)).thenReturn(Optional.of(culto));
        doThrow(new BusinessException("Sem acesso a esta igreja")).when(autorizacaoService).assertAcesso(igrejaId);

        assertThatThrownBy(() -> cultoService.buscarPorId(id)).isInstanceOf(BusinessException.class);
    }

    @Test
    void naoDeveListarCultosQuandoNaoHaIgrejasNoEscopo() {
        when(autorizacaoService.idsParaListagem(null)).thenReturn(List.of());

        assertThat(cultoService.listar(null, null, null, null, null, null, null)).isEmpty();
        verify(cultoRepository, never()).filtrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deveRejeitarListagemComPeriodoInvalido() {
        UUID igrejaId = UUID.randomUUID();
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));

        assertThatThrownBy(() -> cultoService.listar(
                igrejaId, null, null, null, LocalDate.of(2026, 8, 31), LocalDate.of(2026, 8, 1), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Data fim deve ser igual ou posterior");
    }

    @Test
    void deveMarcarCultosQueJaPossuemRelatorio() {
        UUID igrejaId = UUID.randomUUID();
        Culto comRelatorio = culto(igrejaId, StatusCulto.REALIZADO);
        Culto semRelatorio = culto(igrejaId, StatusCulto.AGENDADO);

        CultoRelatorio relatorio = new CultoRelatorio();
        relatorio.setCultoId(comRelatorio.getId());

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(cultoRepository.filtrar(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(comRelatorio, semRelatorio));
        when(relatorioRepository.findByCultoIdIn(any())).thenReturn(List.of(relatorio));
        when(categoriaRepository.findAllById(any())).thenReturn(List.of(categoria(igrejaId)));

        List<CultoResponse> cultos = cultoService.listar(igrejaId, null, null, null, null, null, null);

        assertThat(cultos.get(0).temRelatorio()).isTrue();
        assertThat(cultos.get(1).temRelatorio()).isFalse();
    }

    @Test
    void deveExcluirCultoSemRelatorio() {
        UUID id = UUID.randomUUID();
        Culto culto = culto(UUID.randomUUID(), StatusCulto.AGENDADO);

        when(cultoRepository.findById(id)).thenReturn(Optional.of(culto));
        when(relatorioRepository.existsByCultoId(id)).thenReturn(false);

        cultoService.excluir(id);

        verify(cultoRepository).delete(culto);
    }

    @Test
    void deveDevolverRelatorioVazioQuandoAindaNaoLancado() {
        UUID igrejaId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.ABERTO);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));
        when(relatorioRepository.findByCultoId(culto.getId())).thenReturn(Optional.empty());

        CultoRelatorioResponse response = cultoService.obterRelatorio(culto.getId());

        assertThat(response.cultoId()).isEqualTo(culto.getId());
        assertThat(response.totalPresentes()).isZero();
        assertThat(response.totalVeiculos()).isZero();
    }

    @Test
    void naoDeveLancarRelatorioDeCultoCancelado() {
        UUID igrejaId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.CANCELADO);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));

        assertThatThrownBy(() -> cultoService.salvarRelatorio(culto.getId(),
                new CultoRelatorioRequest(10, 5, 1, 1, 1, 1, 0, 0, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não é possível lançar relatório de um culto cancelado");
        verify(relatorioRepository, never()).save(any());
    }

    @Test
    void deveRejeitarRelatorioComValoresNegativos() {
        UUID igrejaId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.ABERTO);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));

        assertThatThrownBy(() -> cultoService.salvarRelatorio(culto.getId(),
                new CultoRelatorioRequest(10, 5, 1, 1, 1, -1, 0, 0, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Os valores do relatório não podem ser negativos");
        verify(relatorioRepository, never()).save(any());
    }

    @Test
    void deveTratarValoresNulosDoRelatorioComoZero() {
        UUID igrejaId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.ABERTO);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));
        when(relatorioRepository.findByCultoId(culto.getId())).thenReturn(Optional.empty());
        when(relatorioRepository.save(any())).thenAnswer(invocation -> {
            CultoRelatorio r = invocation.getArgument(0);
            r.setCreatedAt(Instant.now());
            r.setUpdatedAt(Instant.now());
            return r;
        });
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CultoRelatorioResponse response = cultoService.salvarRelatorio(culto.getId(),
                new CultoRelatorioRequest(null, null, null, null, null, null, null, null, null));

        assertThat(response.adultos()).isZero();
        assertThat(response.criancas()).isZero();
        assertThat(response.totalVeiculos()).isZero();
    }

    @Test
    void deveAtualizarRelatorioJaLancadoSemDuplicarRegistro() {
        UUID igrejaId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.REALIZADO);

        CultoRelatorio relatorio = new CultoRelatorio();
        relatorio.setId(UUID.randomUUID());
        relatorio.setCultoId(culto.getId());
        relatorio.setAdultos(10);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));
        when(relatorioRepository.findByCultoId(culto.getId())).thenReturn(Optional.of(relatorio));
        when(relatorioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CultoRelatorioResponse response = cultoService.salvarRelatorio(culto.getId(),
                new CultoRelatorioRequest(25, 5, 3, 2, 1, 4, 1, 2, "Relatório revisado"));

        assertThat(response.adultos()).isEqualTo(25);
        assertThat(response.observacoes()).isEqualTo("Relatório revisado");
        verify(relatorioRepository).save(relatorio);
    }

    @Test
    void naoDeveAlterarPregadorPorQuemNaoEPastorOuAdmin() {
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorId = UUID.randomUUID();
        UUID novoPregadorId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.ABERTO);
        culto.setPregadorId(pregadorId);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));
        when(autorizacaoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(autorizacaoService.podeEditarTexto(igrejaId, pregadorId)).thenReturn(true);

        assertThatThrownBy(() -> cultoService.salvarConteudo(culto.getId(),
                new CultoConteudoUpdateRequest(null, null, null, novoPregadorId)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Somente pastor ou admin pode alterar o pregador");
        verify(cultoRepository, never()).save(any(Culto.class));
    }

    @Test
    void deveHerdarPregadorPadraoDaCategoriaQuandoCultoNaoTemPregador() {
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorPadraoId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.ABERTO);
        CategoriaCulto categoria = categoria(igrejaId);
        categoria.setPregadorPadraoId(pregadorPadraoId);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));
        when(autorizacaoService.isPastorOuAdmin(igrejaId)).thenReturn(true);
        when(autorizacaoService.podeEditarTexto(igrejaId, pregadorPadraoId)).thenReturn(true);
        when(categoriaRepository.findById(culto.getCategoriaCultoId())).thenReturn(Optional.of(categoria));
        when(cultoRepository.save(any(Culto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pessoaRepository.findById(pregadorPadraoId)).thenReturn(Optional.of(pregador(pregadorPadraoId, "Pr. Daniel")));

        CultoConteudoResponse response = cultoService.salvarConteudo(culto.getId(),
                new CultoConteudoUpdateRequest("Mensagem", null, null, null));

        assertThat(response.pregadorId()).isEqualTo(pregadorPadraoId);
        assertThat(response.pregadorNome()).isEqualTo("Pr. Daniel");
    }

    @Test
    void naoDeveAplicarTemaDeQuemNaoPodeEditarOConteudo() {
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorId = UUID.randomUUID();
        Culto culto = culto(igrejaId, StatusCulto.ABERTO);
        culto.setPregadorId(pregadorId);

        when(cultoRepository.findById(culto.getId())).thenReturn(Optional.of(culto));
        when(autorizacaoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(autorizacaoService.podeEditarTexto(igrejaId, pregadorId)).thenReturn(false);
        when(autorizacaoService.podeEditarLouvor(igrejaId)).thenReturn(false);

        assertThatThrownBy(() -> cultoService.salvarConteudo(culto.getId(),
                new CultoConteudoUpdateRequest(null, null, "Tema forjado", null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Pessoa pregador(UUID id, String nome) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(id);
        pessoa.setNome(nome);
        pessoa.setAtivo(true);
        return pessoa;
    }

    @Test
    void deveConsolidarContagemDoMesIgnorandoCultosCancelados() {
        UUID igrejaId = UUID.randomUUID();
        Culto primeiro = culto(igrejaId, StatusCulto.REALIZADO);
        Culto segundo = culto(igrejaId, StatusCulto.REALIZADO);
        Culto semRelatorio = culto(igrejaId, StatusCulto.AGENDADO);
        Culto cancelado = culto(igrejaId, StatusCulto.CANCELADO);
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(cultoRepository.filtrar(List.of(igrejaId), null, "2026-08", null, null, null, true))
                .thenReturn(List.of(primeiro, segundo, semRelatorio, cancelado));
        when(relatorioRepository.findByCultoIdIn(List.of(primeiro.getId(), segundo.getId(), semRelatorio.getId())))
                .thenReturn(List.of(
                        relatorio(primeiro.getId(), 100, 20, 3, 40),
                        relatorio(segundo.getId(), 81, 10, 1, 30)
                ));

        CultoRelatorioConsolidadoResponse consolidado =
                cultoService.relatorioConsolidado(igrejaId, null, "2026-08", null, null);

        assertThat(consolidado.totalCultos()).isEqualTo(3);
        assertThat(consolidado.cultosComRelatorio()).isEqualTo(2);
        assertThat(consolidado.cultosSemRelatorio()).isEqualTo(1);
        assertThat(consolidado.adultos()).isEqualTo(181);
        assertThat(consolidado.criancas()).isEqualTo(30);
        assertThat(consolidado.totalPresentes()).isEqualTo(211);
        assertThat(consolidado.conversoes()).isEqualTo(4);
        assertThat(consolidado.totalVeiculos()).isEqualTo(70);
        assertThat(consolidado.mediaPresentes()).isEqualTo(105.5);
        assertThat(consolidado.mediaVeiculos()).isEqualTo(35.0);
        assertThat(consolidado.cultos()).hasSize(3);
        assertThat(consolidado.cultos().get(2).temRelatorio()).isFalse();
        assertThat(consolidado.cultos().get(2).relatorio()).isNull();
        assertThat(consolidado.cultos().get(0).relatorio().totalPresentes()).isEqualTo(120);
    }

    @Test
    void deveDevolverConsolidadoZeradoSemIgrejasNoEscopo() {
        when(autorizacaoService.idsParaListagem(null)).thenReturn(List.of());

        CultoRelatorioConsolidadoResponse consolidado =
                cultoService.relatorioConsolidado(null, null, "2026-08", null, null);

        assertThat(consolidado.totalCultos()).isZero();
        assertThat(consolidado.mediaPresentes()).isZero();
        assertThat(consolidado.cultos()).isEmpty();
        verify(cultoRepository, never()).filtrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deveRejeitarConsolidadoComCompetenciaInvalida() {
        assertThatThrownBy(() -> cultoService.relatorioConsolidado(null, null, "08/2026", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("YYYY-MM");
    }

    private CultoRelatorio relatorio(UUID cultoId, int adultos, int criancas, int conversoes, int carros) {
        CultoRelatorio relatorio = CultoRelatorio.vazio(cultoId);
        relatorio.setAdultos(adultos);
        relatorio.setCriancas(criancas);
        relatorio.setConversoes(conversoes);
        relatorio.setCarros(carros);
        return relatorio;
    }
}
