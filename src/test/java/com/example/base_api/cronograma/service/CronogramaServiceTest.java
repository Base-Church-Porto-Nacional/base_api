package com.example.base_api.cronograma.service;

import com.example.base_api.cronograma.dto.CronogramaConteudoResponse;
import com.example.base_api.cronograma.dto.CronogramaConteudoUpdateRequest;
import com.example.base_api.cronograma.dto.CronogramaEventoCreateRequest;
import com.example.base_api.cronograma.dto.CronogramaEventoResponse;
import com.example.base_api.cronograma.entity.CronogramaConteudo;
import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.entity.TipoEvento;
import com.example.base_api.cronograma.repository.CronogramaConteudoRepository;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.service.IgrejaContextoService;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CronogramaServiceTest {

    @Mock
    private CronogramaEventoRepository repository;
    @Mock
    private CronogramaConteudoRepository conteudoRepository;
    @Mock
    private IgrejaContextoService igrejaContextoService;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private EscalaRepository escalaRepository;
    @Mock
    private EscalaVoluntarioRepository escalaVoluntarioRepository;

    @InjectMocks
    private CronogramaService cronogramaService;

    @Test
    void deveCriarEventoNoCronograma() {
        UUID igrejaId = UUID.randomUUID();
        UUID pastorId = UUID.randomUUID();
        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setPastorPessoaId(pastorId);

        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja);
        when(repository.save(any(CronogramaEvento.class))).thenAnswer(invocation -> {
            CronogramaEvento e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });

        CronogramaEventoResponse response = cronogramaService.criar(new CronogramaEventoCreateRequest(
                "Culto de Domingo",
                TipoEvento.CULTO,
                null,
                LocalDate.of(2026, 8, 24),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                "Templo",
                igrejaId,
                null
        ));

        assertThat(response.titulo()).isEqualTo("Culto de Domingo");
        assertThat(response.tipo()).isEqualTo(TipoEvento.CULTO);
        assertThat(response.ativo()).isTrue();
        assertThat(response.igrejaId()).isEqualTo(igrejaId);
        assertThat(response.supervisorPessoaId()).isEqualTo(pastorId);
    }

    @Test
    void deveRetornarConteudoVazioQuandoNaoExiste() {
        UUID eventoId = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        UUID pastorId = UUID.randomUUID();
        CronogramaEvento evento = new CronogramaEvento();
        evento.setId(eventoId);
        evento.setIgrejaId(igrejaId);
        evento.setDataEvento(LocalDate.of(2026, 8, 9));

        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setPastorPessoaId(pastorId);

        when(repository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(conteudoRepository.findByEventoId(eventoId)).thenReturn(Optional.empty());
        when(escalaRepository.findByIgrejaIdAndCompetenciaAndTipo(any(), any(), any())).thenReturn(List.of());
        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja);

        CronogramaConteudoResponse response = cronogramaService.obterConteudo(eventoId);

        assertThat(response.eventoId()).isEqualTo(eventoId);
        assertThat(response.textoPregacao()).isEmpty();
        assertThat(response.louvor()).isEmpty();
        assertThat(response.pregadorPessoaId()).isEqualTo(pastorId);
    }

    @Test
    void deveSalvarConteudoDoCulto() {
        UUID eventoId = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        CronogramaEvento evento = new CronogramaEvento();
        evento.setId(eventoId);
        evento.setIgrejaId(igrejaId);
        evento.setDataEvento(LocalDate.of(2026, 8, 9));

        when(repository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(conteudoRepository.findByEventoId(eventoId)).thenReturn(Optional.empty());
        when(autorizacaoService.podeEditarTexto(igrejaId, null)).thenReturn(true);
        when(autorizacaoService.podeEditarLouvor(igrejaId)).thenReturn(true);
        when(escalaRepository.findByIgrejaIdAndCompetenciaAndTipo(any(), any(), any())).thenReturn(List.of());
        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        when(igrejaContextoService.buscarIgreja(igrejaId)).thenReturn(igreja);
        when(conteudoRepository.save(any(CronogramaConteudo.class))).thenAnswer(invocation -> {
            CronogramaConteudo c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CronogramaConteudoResponse response = cronogramaService.salvarConteudo(
                eventoId,
                new CronogramaConteudoUpdateRequest("João 15", "Bondade de Deus", null)
        );

        assertThat(response.textoPregacao()).isEqualTo("João 15");
        assertThat(response.louvor()).isEqualTo("Bondade de Deus");
    }

    @Test
    void spoofDePregadorNaoLiberaTexto() {
        UUID eventoId = UUID.randomUUID();
        UUID igrejaId = UUID.randomUUID();
        UUID pregadorPersistido = UUID.randomUUID();
        UUID spoofId = UUID.randomUUID();

        CronogramaEvento evento = new CronogramaEvento();
        evento.setId(eventoId);
        evento.setIgrejaId(igrejaId);
        evento.setDataEvento(LocalDate.of(2026, 8, 9));

        CronogramaConteudo conteudo = new CronogramaConteudo();
        conteudo.setEventoId(eventoId);
        conteudo.setPregadorPessoaId(pregadorPersistido);
        conteudo.setTextoPregacao("texto original");

        when(repository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(conteudoRepository.findByEventoId(eventoId)).thenReturn(Optional.of(conteudo));
        when(autorizacaoService.podeEditarTexto(igrejaId, pregadorPersistido)).thenReturn(false);
        when(autorizacaoService.podeEditarLouvor(igrejaId)).thenReturn(false);

        assertThatThrownBy(() -> cronogramaService.salvarConteudo(
                eventoId,
                new CronogramaConteudoUpdateRequest("texto spoof", null, spoofId)
        )).isInstanceOf(AccessDeniedException.class);

        verify(autorizacaoService).podeEditarTexto(igrejaId, pregadorPersistido);
        verify(autorizacaoService, never()).podeEditarTexto(igrejaId, spoofId);
        verify(conteudoRepository, never()).save(any());
    }
}
