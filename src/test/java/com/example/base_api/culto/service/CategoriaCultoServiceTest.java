package com.example.base_api.culto.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.culto.dto.CategoriaCultoCreateRequest;
import com.example.base_api.culto.dto.CategoriaCultoResponse;
import com.example.base_api.culto.entity.CategoriaCulto;
import com.example.base_api.culto.entity.DiaSemana;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
}