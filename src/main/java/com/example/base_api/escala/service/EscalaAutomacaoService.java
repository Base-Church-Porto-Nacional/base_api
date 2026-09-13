package com.example.base_api.escala.service;

import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaServico;
import com.example.base_api.escala.entity.StatusEscala;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Automação do dia 20: gera as escalas recomendadas (status SUGERIDA) do próximo
 * mês para cada igreja — culto (serviços transversais) e um por ministério.
 * Nunca sobrescreve designações feitas pelo responsável; apenas preenche lacunas
 * em escalas ainda em SUGERIDA/EDITANDO.
 */
@Service
public class EscalaAutomacaoService {

    private static final Logger log = LoggerFactory.getLogger(EscalaAutomacaoService.class);
    private static final String[] MESES = {
            "", "janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    };

    private final GeracaoEscalaService geracaoEscalaService;
    private final EscalaRepository escalaRepository;
    private final EscalaServicoRepository escalaServicoRepository;
    private final IgrejaRepository igrejaRepository;
    private final MinisterioRepository ministerioRepository;
    private final ServicoRepository servicoRepository;
    private final CronogramaEventoRepository cronogramaEventoRepository;
    private final PessoaServicoRepository pessoaServicoRepository;
    private final AutorizacaoService autorizacaoService;

    public EscalaAutomacaoService(
            GeracaoEscalaService geracaoEscalaService,
            EscalaRepository escalaRepository,
            EscalaServicoRepository escalaServicoRepository,
            IgrejaRepository igrejaRepository,
            MinisterioRepository ministerioRepository,
            ServicoRepository servicoRepository,
            CronogramaEventoRepository cronogramaEventoRepository,
            PessoaServicoRepository pessoaServicoRepository,
            AutorizacaoService autorizacaoService
    ) {
        this.geracaoEscalaService = geracaoEscalaService;
        this.escalaRepository = escalaRepository;
        this.escalaServicoRepository = escalaServicoRepository;
        this.igrejaRepository = igrejaRepository;
        this.ministerioRepository = ministerioRepository;
        this.servicoRepository = servicoRepository;
        this.cronogramaEventoRepository = cronogramaEventoRepository;
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.autorizacaoService = autorizacaoService;
    }

    @Scheduled(cron = "0 30 8 20 * *")
    @Transactional
    public String gerarProximoMesAgendado() {
        return gerarProximoMes();
    }

    @Transactional
    public String gerarProximoMesManual() {
        autorizacaoService.assertMatrizPastorOuAdmin();
        return gerarProximoMes();
    }

    @Transactional
    public String gerarProximoMes() {
        String competencia = YearMonth.now().plusMonths(1).toString();
        int criadas = 0;
        int atualizadas = 0;

        for (Igreja igreja : igrejaRepository.findByAtivoTrueOrderByNomeAsc()) {
            if (!temEventosNoMes(igreja.getId(), competencia)) {
                continue;
            }
            Resultado culto = processarCulto(igreja, competencia);
            criadas += culto == Resultado.CRIADA ? 1 : 0;
            atualizadas += culto == Resultado.ATUALIZADA ? 1 : 0;
            for (Ministerio ministerio : ministerioRepository.findByIgrejaIdOrderByNomeAsc(igreja.getId())) {
                Resultado resultado = processarMinisterio(igreja, ministerio, competencia);
                criadas += resultado == Resultado.CRIADA ? 1 : 0;
                atualizadas += resultado == Resultado.ATUALIZADA ? 1 : 0;
            }
        }
        String resumo = "Escalas recomendadas de " + competencia + " geradas: " + criadas
                + " criadas, " + atualizadas + " atualizadas.";
        log.info(resumo);
        return resumo;
    }

    private Resultado processarCulto(Igreja igreja, String competencia) {
        Optional<Escala> existente = escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipo(
                igreja.getId(), competencia, TipoEscala.CULTO);
        if (existente.isPresent()) {
            return regenerarSeRascunho(existente.get());
        }
        List<UUID> ministerioIds = ministerioRepository.findByIgrejaIdOrderByNomeAsc(igreja.getId())
                .stream()
                .map(Ministerio::getId)
                .toList();
        List<Servico> servicos = servicoRepository.findByMinisterioIdInAndAtivoTrueOrderByNomeAsc(ministerioIds)
                .stream()
                .filter(s -> !Boolean.TRUE.equals(s.getFiccao()))
                .filter(this::temVoluntario)
                .toList();
        if (servicos.isEmpty()) {
            return Resultado.NADA;
        }
        Escala escala = novaEscala(igreja, competencia, "Escala de culto", TipoEscala.CULTO, null);
        for (Servico servico : servicos) {
            salvarNecessidade(escala, servico.getId());
        }
        geracaoEscalaService.gerar(escala);
        return Resultado.CRIADA;
    }

    private Resultado processarMinisterio(Igreja igreja, Ministerio ministerio, String competencia) {
        Optional<Escala> existente = escalaRepository.findFirstByIgrejaIdAndCompetenciaAndTipo(
                igreja.getId(), competencia, TipoEscala.MINISTERIO);
        if (existente.isPresent()) {
            return regenerarSeRascunho(existente.get());
        }
        List<UUID> servicoIds = servicoRepository.findByMinisterioIdOrderByNomeAsc(ministerio.getId())
                .stream()
                .filter(s -> Boolean.TRUE.equals(s.getAtivo()))
                .filter(s -> !Boolean.TRUE.equals(s.getFiccao()))
                .filter(this::temVoluntario)
                .map(Servico::getId)
                .toList();
        if (servicoIds.isEmpty()) {
            return Resultado.NADA;
        }
        Escala escala = novaEscala(igreja, competencia, ministerio.getNome(), TipoEscala.MINISTERIO, ministerio.getId());
        for (UUID servicoId : servicoIds) {
            salvarNecessidade(escala, servicoId);
        }
        geracaoEscalaService.gerar(escala);
        return Resultado.CRIADA;
    }

    private Resultado regenerarSeRascunho(Escala escala) {
        if (escala.getStatus() == StatusEscala.SUGERIDA || escala.getStatus() == StatusEscala.EDITANDO) {
            geracaoEscalaService.gerar(escala);
            return Resultado.ATUALIZADA;
        }
        return Resultado.NADA;
    }

    private Escala novaEscala(
            Igreja igreja,
            String competencia,
            String nome,
            TipoEscala tipo,
            UUID ministerioId
    ) {
        Escala escala = new Escala();
        escala.setTitulo(nome + " — " + nomeMes(competencia));
        escala.setCompetencia(competencia);
        escala.setTipo(tipo);
        escala.setIgrejaId(igreja.getId());
        escala.setMinisterioId(ministerioId);
        escala.setDataEvento(YearMonth.parse(competencia).atDay(1));
        escala.setHoraInicio(LocalTime.MIDNIGHT);
        escala.setSupervisorPessoaId(igreja.getPastorPessoaId());
        escala.setPregadorPessoaId(igreja.getPastorPessoaId());
        escala.setAtivo(true);
        escala.setStatus(StatusEscala.SUGERIDA);
        return escalaRepository.save(escala);
    }

    private void salvarNecessidade(Escala escala, UUID servicoId) {
        EscalaServico necessidade = new EscalaServico();
        necessidade.setEscalaId(escala.getId());
        necessidade.setServicoId(servicoId);
        necessidade.setQuantidadeNecessaria(1);
        escalaServicoRepository.save(necessidade);
    }

    private boolean temEventosNoMes(UUID igrejaId, String competencia) {
        YearMonth mes = YearMonth.parse(competencia);
        return cronogramaEventoRepository
                .findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                        List.of(igrejaId), mes.atDay(1), mes.atEndOfMonth())
                .stream()
                .anyMatch(e -> Boolean.TRUE.equals(e.getAtivo()));
    }

    private boolean temVoluntario(Servico servico) {
        return !pessoaServicoRepository.findByServicoIdAndAtivoTrue(servico.getId()).isEmpty();
    }

    private String nomeMes(String competencia) {
        return MESES[YearMonth.parse(competencia).getMonthValue()] + " " + YearMonth.parse(competencia).getYear();
    }

    private enum Resultado {
        CRIADA,
        ATUALIZADA,
        NADA
    }
}