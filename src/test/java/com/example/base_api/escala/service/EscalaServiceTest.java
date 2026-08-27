package com.example.base_api.escala.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.cronograma.service.CronogramaService;
import com.example.base_api.escala.dto.EscalaCreateRequest;
import com.example.base_api.escala.dto.EscalaResponse;
import com.example.base_api.escala.dto.EscalaVoluntarioCreateRequest;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaServico;
import com.example.base_api.escala.entity.EscalaVoluntario;
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
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private EscalaService escalaService;

    private UUID escalaId;
    private UUID servicoId;
    private UUID pessoaId;
    private UUID igrejaId;
    private UUID pastorId;

    @BeforeEach
    void setUp() {
        escalaId = UUID.randomUUID();
        servicoId = UUID.randomUUID();
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
        servico.setMinisterioId(UUID.randomUUID());
        servico.setFiccao(false);
        return servico;
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
}
