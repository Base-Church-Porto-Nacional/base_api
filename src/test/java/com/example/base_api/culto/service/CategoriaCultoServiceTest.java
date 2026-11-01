package com.example.base_api.culto.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.culto.dto.CategoriaCultoCreateRequest;
import com.example.base_api.culto.dto.CategoriaCultoResponse;
import com.example.base_api.culto.dto.CategoriaCultoUpdateRequest;
import com.example.base_api.culto.entity.CategoriaCulto;
import com.example.base_api.common.entity.DiaSemana;
import com.example.base_api.culto.repository.CategoriaCultoRepository;
import com.example.base_api.culto.repository.CultoRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaCultoServiceTest {

    @Mock
    private CategoriaCultoRepository categoriaRepository;
    @Mock
    private CultoRepository cultoRepository;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private PessoaRepository pessoaRepository;

    @InjectMocks
    private CategoriaCultoService categoriaService;

    @Test
    void deveCriarCategoriaComPadroesQuandoNaoInformados() {
        UUID igrejaId = UUID.randomUUID();
        when(categoriaRepository.existsByIgrejaIdAndTituloIgnoreCase(igrejaId, "Domingo inesquecível"))
                .thenReturn(false);
        when(categoriaRepository.save(any(CategoriaCulto.class))).thenAnswer(invocation -> {
            CategoriaCulto c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });

        CategoriaCultoResponse response = categoriaService.criar(new CategoriaCultoCreateRequest(
                "Domingo inesquecível", null, DiaSemana.DOMINGO, null, null, null, igrejaId
        ));

        assertThat(response.titulo()).isEqualTo("Domingo inesquecível");
        assertThat(response.diaSemana()).isEqualTo(DiaSemana.DOMINGO);
        assertThat(response.horaInicio()).isEqualTo(LocalTime.of(20, 0));
        assertThat(response.duracaoMinutos()).isEqualTo(90);
        assertThat(response.igrejaId()).isEqualTo(igrejaId);
        assertThat(response.ativo()).isTrue();
    }

    @Test
    void deveUsarIgrejaDoUsuarioQuandoNaoInformada() {
        UUID igrejaId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setIgrejaId(igrejaId);
        when(autorizacaoService.requireUsuario()).thenReturn(usuario);
        when(categoriaRepository.existsByIgrejaIdAndTituloIgnoreCase(igrejaId, "Domingo inesquecível"))
                .thenReturn(false);
        when(categoriaRepository.save(any(CategoriaCulto.class))).thenAnswer(invocation -> {
            CategoriaCulto c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });

        CategoriaCultoResponse response = categoriaService.criar(new CategoriaCultoCreateRequest(
                "Domingo inesquecível", null, DiaSemana.DOMINGO, null, null, null, null
        ));

        assertThat(response.igrejaId()).isEqualTo(igrejaId);
    }

    @Test
    void deveRejeitarCategoriaComTituloDuplicadoNaMesmaIgreja() {
        UUID igrejaId = UUID.randomUUID();
        when(categoriaRepository.existsByIgrejaIdAndTituloIgnoreCase(igrejaId, "Domingo inesquecível"))
                .thenReturn(true);

        assertThatThrownBy(() -> categoriaService.criar(new CategoriaCultoCreateRequest(
                "Domingo inesquecível", null, DiaSemana.DOMINGO, null, null, null, igrejaId
        ))).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveRejeitarPregadorPadraoDeOutraIgreja() {
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorId = UUID.randomUUID();
        Pessoa pessoa = new Pessoa();
        pessoa.setId(pregadorId);
        pessoa.setIgrejaId(UUID.randomUUID());

        when(pessoaRepository.findById(pregadorId)).thenReturn(Optional.of(pessoa));
        when(categoriaRepository.existsByIgrejaIdAndTituloIgnoreCase(igrejaId, "Domingo inesquecível"))
                .thenReturn(false);
        doThrow(new BusinessException("A pessoa não pertence a esta igreja"))
                .when(autorizacaoService).assertPessoaDaIgreja(eq(pessoa), eq(igrejaId));

        assertThatThrownBy(() -> categoriaService.criar(new CategoriaCultoCreateRequest(
                "Domingo inesquecível", null, DiaSemana.DOMINGO, null, null, pregadorId, igrejaId
        ))).isInstanceOf(BusinessException.class);
    }

    @Test
    void deveDesativarEmVezDeExcluirQuandoCategoriaPossuiCultos() {
        UUID id = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = new CategoriaCulto();
        categoria.setId(id);
        categoria.setIgrejaId(igrejaId);
        categoria.setAtivo(true);

        when(categoriaRepository.findById(id)).thenReturn(Optional.of(categoria));
        when(cultoRepository.countByCategoriaCultoIdAndAtivoTrue(id)).thenReturn(1L);

        ArgumentCaptor<CategoriaCulto> captor = ArgumentCaptor.forClass(CategoriaCulto.class);
        categoriaService.excluir(id);

        verify(categoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getAtivo()).isFalse();
    }

    @Test
    void deveExcluirCategoriaSemCultosVinculados() {
        UUID id = UUID.randomUUID();
        CategoriaCulto categoria = categoriaExistente(id, UUID.randomUUID());

        when(categoriaRepository.findById(id)).thenReturn(Optional.of(categoria));
        when(cultoRepository.countByCategoriaCultoIdAndAtivoTrue(id)).thenReturn(0L);

        categoriaService.excluir(id);

        verify(categoriaRepository).delete(categoria);
        verify(categoriaRepository, never()).save(any(CategoriaCulto.class));
    }

    @Test
    void naoDeveListarCategoriasQuandoNaoHaIgrejasNoEscopo() {
        UUID igrejaId = UUID.randomUUID();
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of());

        assertThat(categoriaService.listar(igrejaId, null)).isEmpty();
        verify(categoriaRepository, never()).findByIgrejaIdInOrderByTituloAsc(any());
    }

    @Test
    void deveListarSomenteCategoriasAtivasQuandoFiltroAtivo() {
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto ativa = categoriaExistente(UUID.randomUUID(), igrejaId);

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(categoriaRepository.findByIgrejaIdInAndAtivoOrderByTituloAsc(List.of(igrejaId), true))
                .thenReturn(List.of(ativa));

        List<CategoriaCultoResponse> categorias = categoriaService.listar(igrejaId, true);

        assertThat(categorias).singleElement()
                .extracting(CategoriaCultoResponse::titulo)
                .isEqualTo("Domingo inesquecível");
        verify(categoriaRepository, never()).findByIgrejaIdInOrderByTituloAsc(any());
    }

    @Test
    void deveRecusarConsultaDeCategoriaForaDoAcesso() {
        UUID id = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = categoriaExistente(id, igrejaId);

        when(categoriaRepository.findById(id)).thenReturn(Optional.of(categoria));
        doThrow(new BusinessException("Sem acesso a esta igreja"))
                .when(autorizacaoService).assertAcesso(igrejaId);

        assertThatThrownBy(() -> categoriaService.buscarPorId(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deveRejeitarCriacaoSemIgrejaDoUsuarioNemDoRequest() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        when(autorizacaoService.requireUsuario()).thenReturn(usuario);

        assertThatThrownBy(() -> categoriaService.criar(new CategoriaCultoCreateRequest(
                "Domingo inesquecível", null, DiaSemana.DOMINGO, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("igrejaId é obrigatório");
    }

    @Test
    void naoDeveCriarCategoriaParaIgrejaForaDoAcessoDoUsuario() {
        UUID igrejaId = UUID.randomUUID();
        doThrow(new BusinessException("Sem permissão para gerenciar esta igreja"))
                .when(autorizacaoService).assertPastorOuAdmin(igrejaId);

        assertThatThrownBy(() -> categoriaService.criar(new CategoriaCultoCreateRequest(
                "Domingo inesquecível", null, DiaSemana.DOMINGO, null, null, null, igrejaId)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Sem permissão para gerenciar");
        verify(categoriaRepository, never()).save(any(CategoriaCulto.class));
    }

    @Test
    void deveAtualizarCategoriaMantendoTituloEValoresInformados() {
        UUID id = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorId = UUID.randomUUID();
        CategoriaCulto categoria = categoriaExistente(id, igrejaId);
        Pessoa pregador = new Pessoa();
        pregador.setId(pregadorId);
        pregador.setNome("Pr. Daniel");
        pregador.setIgrejaId(igrejaId);

        when(categoriaRepository.findById(id)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.existsByIgrejaIdAndTituloIgnoreCaseAndIdNot(igrejaId, "Culto da Família", id))
                .thenReturn(false);
        when(pessoaRepository.findById(pregadorId)).thenReturn(Optional.of(pregador));
        when(categoriaRepository.save(any(CategoriaCulto.class))).thenAnswer(invocation -> {
            CategoriaCulto c = invocation.getArgument(0);
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });

        CategoriaCultoResponse response = categoriaService.atualizar(id, new CategoriaCultoUpdateRequest(
                " Culto da Família ", "Família reunida", DiaSemana.SABADO,
                LocalTime.of(19, 0), 120, pregadorId, true));

        assertThat(response.titulo()).isEqualTo("Culto da Família");
        assertThat(response.diaSemana()).isEqualTo(DiaSemana.SABADO);
        assertThat(response.horaInicio()).isEqualTo(LocalTime.of(19, 0));
        assertThat(response.duracaoMinutos()).isEqualTo(120);
        assertThat(response.pregadorPadraoNome()).isEqualTo("Pr. Daniel");
    }

    @Test
    void deveRejeitarAtualizacaoParaTituloDeOutraCategoria() {
        UUID id = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = categoriaExistente(id, igrejaId);

        when(categoriaRepository.findById(id)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.existsByIgrejaIdAndTituloIgnoreCaseAndIdNot(igrejaId, "Domingo inesquecível", id))
                .thenReturn(true);

        assertThatThrownBy(() -> categoriaService.atualizar(id, new CategoriaCultoUpdateRequest(
                "Domingo inesquecível", null, DiaSemana.DOMINGO, null, null, null, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe uma categoria de culto com este título");
        verify(categoriaRepository, never()).save(any(CategoriaCulto.class));
    }

    @Test
    void deveRejeitarPregadorPadraoDeIgrejaAlheiaNaAtualizacao() {
        UUID id = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorId = UUID.randomUUID();
        CategoriaCulto categoria = categoriaExistente(id, igrejaId);
        Pessoa alheia = new Pessoa();
        alheia.setId(pregadorId);
        alheia.setIgrejaId(UUID.randomUUID());

        when(categoriaRepository.findById(id)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.existsByIgrejaIdAndTituloIgnoreCaseAndIdNot(any(), any(), any()))
                .thenReturn(false);
        when(pessoaRepository.findById(pregadorId)).thenReturn(Optional.of(alheia));
        doThrow(new BusinessException("A pessoa não pertence a esta igreja"))
                .when(autorizacaoService).assertPessoaDaIgreja(alheia, igrejaId);

        assertThatThrownBy(() -> categoriaService.atualizar(id, new CategoriaCultoUpdateRequest(
                "Culto da Família", null, DiaSemana.DOMINGO, null, null, pregadorId, true)))
                .isInstanceOf(BusinessException.class);
        verify(categoriaRepository, never()).save(any(CategoriaCulto.class));
    }

    @Test
    void deveRejeitarAtualizacaoDeCategoriaForaDoAcessoDoUsuario() {
        UUID id = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        CategoriaCulto categoria = categoriaExistente(id, igrejaId);

        when(categoriaRepository.findById(id)).thenReturn(Optional.of(categoria));
        doThrow(new BusinessException("Sem permissão para gerenciar esta igreja"))
                .when(autorizacaoService).assertPastorOuAdmin(igrejaId);

        assertThatThrownBy(() -> categoriaService.atualizar(id, new CategoriaCultoUpdateRequest(
                "Culto da Família", null, DiaSemana.DOMINGO, null, null, null, true)))
                .isInstanceOf(BusinessException.class);
    }

    private CategoriaCulto categoriaExistente(UUID id, UUID igrejaId) {
        CategoriaCulto categoria = new CategoriaCulto();
        categoria.setId(id);
        categoria.setIgrejaId(igrejaId);
        categoria.setTitulo("Domingo inesquecível");
        categoria.setDiaSemana(DiaSemana.DOMINGO);
        categoria.setHoraInicio(LocalTime.of(20, 0));
        categoria.setDuracaoMinutos(90);
        categoria.setAtivo(true);
        return categoria;
    }

    @Test
    void listarBuscaNomesDosPregadoresPadraoNumaUnicaConsulta() {
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorId = UUID.randomUUID();
        CategoriaCulto domingo = categoriaExistente(UUID.randomUUID(), igrejaId);
        domingo.setPregadorPadraoId(pregadorId);
        CategoriaCulto quarta = categoriaExistente(UUID.randomUUID(), igrejaId);

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(categoriaRepository.findByIgrejaIdInOrderByTituloAsc(List.of(igrejaId))).thenReturn(List.of(domingo, quarta));
        when(pessoaRepository.nomesPorId(any())).thenReturn(Map.of(pregadorId, "Pr. Daniel"));

        List<CategoriaCultoResponse> categorias = categoriaService.listar(igrejaId, null);

        assertThat(categorias).extracting(CategoriaCultoResponse::pregadorPadraoNome).containsExactly("Pr. Daniel", null);
        verify(pessoaRepository, times(1)).nomesPorId(any());
        verify(pessoaRepository, never()).findById(any());
    }
}
