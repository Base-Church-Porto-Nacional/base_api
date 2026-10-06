package com.example.base_api.culto.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.culto.entity.CategoriaCulto;
import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.entity.DiaSemana;
import com.example.base_api.culto.entity.StatusCulto;
import com.example.base_api.culto.repository.CategoriaCultoRepository;
import com.example.base_api.culto.repository.CultoRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.security.AutorizacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Automação de cultos: gera as ocorrências de cada categoria de culto ativa no
 * mês (dia da semana), criando instâncias AGENDADO de forma idempotente — nunca
 * sobrescreve nem duplica um culto já existente na data.
 */
@Service
public class CultoAutomacaoService {

    private static final Logger log = LoggerFactory.getLogger(CultoAutomacaoService.class);

    private final CategoriaCultoRepository categoriaRepository;
    private final CultoRepository cultoRepository;
    private final IgrejaRepository igrejaRepository;
    private final AutorizacaoService autorizacaoService;

    public CultoAutomacaoService(
            CategoriaCultoRepository categoriaRepository,
            CultoRepository cultoRepository,
            IgrejaRepository igrejaRepository,
            AutorizacaoService autorizacaoService
    ) {
        this.categoriaRepository = categoriaRepository;
        this.cultoRepository = cultoRepository;
        this.igrejaRepository = igrejaRepository;
        this.autorizacaoService = autorizacaoService;
    }

    @Scheduled(cron = "0 30 8 20 * *", zone = "${app.timezone:America/Sao_Paulo}")
    @Transactional
    public String gerarProximoMesAgendado() {
        String competencia = YearMonth.now().plusMonths(1).toString();
        return gerarParaTodasIgrejas(competencia);
    }

    @Transactional
    public String gerarProximoMesManual(UUID igrejaId) {
        String competencia = YearMonth.now().plusMonths(1).toString();
        return gerarMesManual(competencia, igrejaId);
    }

    @Transactional
    public String gerarMesManual(String competencia, UUID igrejaId) {
        YearMonth mes = parseCompetencia(competencia);
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return "Nenhuma igreja visível para gerar cultos de " + mes;
        }
        for (UUID id : ids) {
            autorizacaoService.assertPastorOuAdmin(id);
        }
        return gerar(ids, mes);
    }

    @Transactional
    public String gerarProximoMesDaCategoria(UUID categoriaId) {
        return gerarMesDaCategoria(categoriaId, YearMonth.now().plusMonths(1).toString());
    }

    @Transactional
    public String gerarMesDaCategoria(UUID categoriaId, String competencia) {
        CategoriaCulto categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria de culto não encontrada"));
        autorizacaoService.assertPastorOuAdmin(categoria.getIgrejaId());
        YearMonth mes = parseCompetencia(competencia);
        int criados = gerarDaCategoria(categoria, mes);
        String resumo = "Cultos de " + categoria.getTitulo() + " em " + mes + ": " + criados + " criados.";
        log.info(resumo);
        return resumo;
    }

    private String gerarParaTodasIgrejas(String competencia) {
        YearMonth mes = parseCompetencia(competencia);
        List<UUID> ids = igrejaRepository.findByAtivoTrueOrderByNomeAsc().stream().map(Igreja::getId).toList();
        if (ids.isEmpty()) {
            return "Nenhuma igreja ativa para gerar cultos de " + mes;
        }
        return gerar(ids, mes);
    }

    private String gerar(List<UUID> igrejaIds, YearMonth mes) {
        int criados = 0;
        int existentes = 0;
        for (CategoriaCulto categoria : categoriaRepository.findByIgrejaIdInAndAtivoOrderByTituloAsc(igrejaIds, true)) {
            int antes = criados;
            criados += gerarDaCategoria(categoria, mes);
            existentes += datasDoMes(categoria.getDiaSemana(), mes).size() - (criados - antes);
        }
        String resumo = "Cultos de " + mes + " gerados: " + criados + " criados, " + existentes + " já existentes.";
        log.info(resumo);
        return resumo;
    }

    private int gerarDaCategoria(CategoriaCulto categoria, YearMonth mes) {
        int criados = 0;
        for (var data : datasDoMes(categoria.getDiaSemana(), mes)) {
            if (cultoRepository.existsByCategoriaCultoIdAndData(categoria.getId(), data)) {
                continue;
            }
            cultoRepository.save(novoCulto(categoria, data));
            criados++;
        }
        return criados;
    }

    private Culto novoCulto(CategoriaCulto categoria, LocalDate data) {
        Culto culto = new Culto();
        culto.setIgrejaId(categoria.getIgrejaId());
        culto.setCategoriaCultoId(categoria.getId());
        culto.setTitulo(categoria.getTitulo());
        culto.setData(data);
        culto.setHoraInicio(categoria.getHoraInicio());
        culto.setHoraFim(calcularHoraFim(categoria));
        culto.setPregadorId(categoria.getPregadorPadraoId());
        culto.setStatus(StatusCulto.AGENDADO);
        culto.setCompetencia(YearMonth.from(data).toString());
        culto.setAtivo(true);
        return culto;
    }

    private LocalTime calcularHoraFim(CategoriaCulto categoria) {
        Integer duracao = categoria.getDuracaoMinutos() != null ? categoria.getDuracaoMinutos() : 90;
        return categoria.getHoraInicio().plusMinutes(duracao);
    }

    private List<LocalDate> datasDoMes(DiaSemana diaSemana, YearMonth mes) {
        DayOfWeek dia = toDayOfWeek(diaSemana);
        return mes.atDay(1).datesUntil(mes.atEndOfMonth().plusDays(1))
                .filter(data -> data.getDayOfWeek() == dia)
                .toList();
    }

    private DayOfWeek toDayOfWeek(DiaSemana diaSemana) {
        return switch (diaSemana) {
            case DOMINGO -> DayOfWeek.SUNDAY;
            case SEGUNDA -> DayOfWeek.MONDAY;
            case TERCA -> DayOfWeek.TUESDAY;
            case QUARTA -> DayOfWeek.WEDNESDAY;
            case QUINTA -> DayOfWeek.THURSDAY;
            case SEXTA -> DayOfWeek.FRIDAY;
            case SABADO -> DayOfWeek.SATURDAY;
        };
    }

    private YearMonth parseCompetencia(String competencia) {
        if (competencia == null || !competencia.matches("\\d{4}-\\d{2}")) {
            throw new BusinessException(
                    "Competência inválida. Use o formato YYYY-MM (ex.: 2026-09)"
            );
        }
        return YearMonth.parse(competencia);
    }
}