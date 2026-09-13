package com.example.base_api.culto.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.culto.dto.CultoConteudoResponse;
import com.example.base_api.culto.dto.CultoConteudoUpdateRequest;
import com.example.base_api.culto.dto.CultoCreateRequest;
import com.example.base_api.culto.dto.CultoRelatorioRequest;
import com.example.base_api.culto.dto.CultoRelatorioResponse;
import com.example.base_api.culto.dto.CultoResponse;
import com.example.base_api.culto.dto.CultoUpdateRequest;
import com.example.base_api.culto.entity.CategoriaCulto;
import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.entity.DiaSemana;
import com.example.base_api.culto.entity.StatusCulto;
import com.example.base_api.culto.repository.CategoriaCultoRepository;
import com.example.base_api.culto.repository.CultoRelatorioRepository;
import com.example.base_api.culto.repository.CultoRepository;
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
}