package com.example.base_api.igreja.service;

import com.example.base_api.cronograma.entity.CronogramaConteudo;
import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.repository.CronogramaConteudoRepository;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.repository.CultoRepository;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipacoesFuturasServiceTest {

    // 02/11/2026 01:30 UTC = 01/11/2026 22:30 em São Paulo: "hoje" é 01/11, não 02/11.
    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-11-02T01:30:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 1);
    private static final String MES_ATUAL = "2026-11";

    @Mock
    private IgrejaRepository igrejaRepository;
    @Mock
    private EscalaRepository escalaRepository;
    @Mock
    private EscalaVoluntarioRepository escalaVoluntarioRepository;
    @Mock
    private CultoRepository cultoRepository;
    @Mock
    private CronogramaEventoRepository cronogramaEventoRepository;
    @Mock
    private CronogramaConteudoRepository cronogramaConteudoRepository;

    private ParticipacoesFuturasService service;

    private final UUID origem = UUID.randomUUID();
    private final UUID pessoaId = UUID.randomUUID();
    private final UUID titularId = UUID.randomUUID();
    private final UUID outraPessoa = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ParticipacoesFuturasService(igrejaRepository, escalaRepository, escalaVoluntarioRepository,
                cultoRepository, cronogramaEventoRepository, cronogramaConteudoRepository, RELOGIO);
        Igreja igreja = new Igreja();
        igreja.setId(origem);
        igreja.setPastorPessoaId(titularId);
        lenient().when(igrejaRepository.findById(origem)).thenReturn(Optional.of(igreja));
    }

    @Test
    void removeDesignacoesFuturasDeVoluntarioDaIgrejaDeOrigem() {
        EscalaVoluntario futura = new EscalaVoluntario();
        futura.setPessoaId(pessoaId);
        when(escalaVoluntarioRepository.findFuturasDaPessoaNaIgreja(pessoaId, origem, HOJE, MES_ATUAL))
                .thenReturn(List.of(futura));

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        // A vaga fica aberta para substituição (mesmo efeito de remover a designação na escala).
        verify(escalaVoluntarioRepository).deleteAll(List.of(futura));
        assertThat(ajustadas).isEqualTo(1);
    }

    @Test
    void consultasUsamHojeNoFusoConfiguradoEOMesAtual() {
        service.ajustarNaSaida(pessoaId, origem);

        // Registros passados ficam de fora já na consulta (data >= hoje / competência >= mês atual).
        verify(escalaVoluntarioRepository).findFuturasDaPessoaNaIgreja(pessoaId, origem, HOJE, MES_ATUAL);
        verify(escalaRepository).findFuturasComSupervisorOuPregador(origem, pessoaId, HOJE, MES_ATUAL);
        verify(cultoRepository).findByIgrejaIdAndPregadorIdAndDataGreaterThanEqual(origem, pessoaId, HOJE);
        verify(cronogramaEventoRepository)
                .findByIgrejaIdAndSupervisorPessoaIdAndDataEventoGreaterThanEqual(origem, pessoaId, HOJE);
        verify(cronogramaConteudoRepository).findFuturosComPregador(origem, pessoaId, HOJE);
    }

    @Test
    void semNadaFuturoNaoAlteraNadaERetornaZero() {
        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        assertThat(ajustadas).isZero();
        verify(escalaVoluntarioRepository, never()).deleteAll(anyList());
        verify(escalaRepository, never()).saveAll(anyList());
        verify(cultoRepository, never()).saveAll(anyList());
        verify(cronogramaEventoRepository, never()).saveAll(anyList());
        verify(cronogramaConteudoRepository, never()).saveAll(anyList());
    }

    @Test
    void cultoFuturoComElaPregandoPassaParaOTitular() {
        Culto culto = new Culto();
        culto.setPregadorId(pessoaId);
        when(cultoRepository.findByIgrejaIdAndPregadorIdAndDataGreaterThanEqual(origem, pessoaId, HOJE))
                .thenReturn(List.of(culto));

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        assertThat(culto.getPregadorId()).isEqualTo(titularId);
        verify(cultoRepository).saveAll(List.of(culto));
        assertThat(ajustadas).isEqualTo(1);
    }

    @Test
    void cronogramaFuturoTrocaSupervisorEPregadorPeloTitular() {
        CronogramaEvento evento = new CronogramaEvento();
        evento.setSupervisorPessoaId(pessoaId);
        CronogramaConteudo conteudo = new CronogramaConteudo();
        conteudo.setPregadorPessoaId(pessoaId);
        when(cronogramaEventoRepository
                .findByIgrejaIdAndSupervisorPessoaIdAndDataEventoGreaterThanEqual(origem, pessoaId, HOJE))
                .thenReturn(List.of(evento));
        when(cronogramaConteudoRepository.findFuturosComPregador(origem, pessoaId, HOJE))
                .thenReturn(List.of(conteudo));

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        assertThat(evento.getSupervisorPessoaId()).isEqualTo(titularId);
        assertThat(conteudo.getPregadorPessoaId()).isEqualTo(titularId);
        verify(cronogramaEventoRepository).saveAll(List.of(evento));
        verify(cronogramaConteudoRepository).saveAll(List.of(conteudo));
        assertThat(ajustadas).isEqualTo(2);
    }

    @Test
    void escalaFuturaTrocaSoOPapelQueElaOcupa() {
        Escala comoSupervisora = new Escala();
        comoSupervisora.setSupervisorPessoaId(pessoaId);
        comoSupervisora.setPregadorPessoaId(outraPessoa);
        Escala comoPregadora = new Escala();
        comoPregadora.setSupervisorPessoaId(outraPessoa);
        comoPregadora.setPregadorPessoaId(pessoaId);
        when(escalaRepository.findFuturasComSupervisorOuPregador(origem, pessoaId, HOJE, MES_ATUAL))
                .thenReturn(List.of(comoSupervisora, comoPregadora));

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        assertThat(comoSupervisora.getSupervisorPessoaId()).isEqualTo(titularId);
        assertThat(comoSupervisora.getPregadorPessoaId()).isEqualTo(outraPessoa);
        assertThat(comoPregadora.getSupervisorPessoaId()).isEqualTo(outraPessoa);
        assertThat(comoPregadora.getPregadorPessoaId()).isEqualTo(titularId);
        verify(escalaRepository).saveAll(List.of(comoSupervisora, comoPregadora));
        assertThat(ajustadas).isEqualTo(2);
    }

    @Test
    void igrejaSemTitularDeixaOPapelVazio() {
        Igreja semTitular = new Igreja();
        semTitular.setId(origem);
        when(igrejaRepository.findById(origem)).thenReturn(Optional.of(semTitular));
        Culto culto = new Culto();
        culto.setPregadorId(pessoaId);
        when(cultoRepository.findByIgrejaIdAndPregadorIdAndDataGreaterThanEqual(origem, pessoaId, HOJE))
                .thenReturn(List.of(culto));

        service.ajustarNaSaida(pessoaId, origem);

        assertThat(culto.getPregadorId()).isNull();
    }

    @Test
    void contaTudoQueFoiAjustado() {
        when(escalaVoluntarioRepository.findFuturasDaPessoaNaIgreja(pessoaId, origem, HOJE, MES_ATUAL))
                .thenReturn(List.of(new EscalaVoluntario(), new EscalaVoluntario()));
        Escala escala = new Escala();
        escala.setSupervisorPessoaId(pessoaId);
        escala.setPregadorPessoaId(pessoaId);
        when(escalaRepository.findFuturasComSupervisorOuPregador(origem, pessoaId, HOJE, MES_ATUAL))
                .thenReturn(List.of(escala));
        Culto culto = new Culto();
        culto.setPregadorId(pessoaId);
        when(cultoRepository.findByIgrejaIdAndPregadorIdAndDataGreaterThanEqual(origem, pessoaId, HOJE))
                .thenReturn(List.of(culto));

        int ajustadas = service.ajustarNaSaida(pessoaId, origem);

        // 2 designações + 1 escala (supervisor e pregador na mesma escala contam uma vez) + 1 culto.
        assertThat(ajustadas).isEqualTo(4);
        assertThat(escala.getSupervisorPessoaId()).isEqualTo(titularId);
        assertThat(escala.getPregadorPessoaId()).isEqualTo(titularId);
    }

    @Test
    void consultaSoAIgrejaDeOrigem() {
        service.ajustarNaSaida(pessoaId, origem);

        verify(escalaVoluntarioRepository, never())
                .findFuturasDaPessoaNaIgreja(any(), org.mockito.ArgumentMatchers.argThat(id -> !origem.equals(id)), any(), any());
        verify(escalaRepository, never())
                .findFuturasComSupervisorOuPregador(org.mockito.ArgumentMatchers.argThat(id -> !origem.equals(id)), any(), any(), any());
        verify(cultoRepository, never())
                .findByIgrejaIdAndPregadorIdAndDataGreaterThanEqual(org.mockito.ArgumentMatchers.argThat(id -> !origem.equals(id)), any(), any());
        verify(cronogramaEventoRepository, never()).findAll();
        verify(cultoRepository, never()).findAll();
        verify(escalaRepository, never()).findAll();
        verify(escalaVoluntarioRepository, never()).findAll();
        verify(cronogramaConteudoRepository, never()).findAll();
        verify(cronogramaConteudoRepository).findFuturosComPregador(eq(origem), eq(pessoaId), eq(HOJE));
    }
}
