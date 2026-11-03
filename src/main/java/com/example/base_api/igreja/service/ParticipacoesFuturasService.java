package com.example.base_api.igreja.service;

import com.example.base_api.cronograma.entity.CronogramaConteudo;
import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.entity.StatusEvento;
import com.example.base_api.cronograma.repository.CronogramaConteudoRepository;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.culto.entity.CategoriaCulto;
import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.entity.StatusCulto;
import com.example.base_api.culto.repository.CategoriaCultoRepository;
import com.example.base_api.culto.repository.CultoRepository;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.entity.StatusEscalaVoluntario;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Quando uma pessoa sai de uma igreja, limpa as participações dela que ainda vão acontecer lá: designações
 * de voluntário são removidas (a vaga fica aberta para substituição), as de supervisor/pregador passam ao
 * titular (ver {@link #ajustarDesignacoes}) e, onde ela supervisiona ou prega,
 * entra o pastor titular da igreja (ou ninguém, se não houver) — inclusive como pregador padrão das categorias
 * de culto, que servem de modelo aos próximos cultos. Registros passados, e cultos/eventos já realizados ou
 * cancelados, nunca são alterados. "Hoje" segue o fuso configurado ({@code app.timezone}).
 */
@Service
public class ParticipacoesFuturasService {

    private static final List<StatusCulto> CULTO_ENCERRADO = List.of(StatusCulto.REALIZADO, StatusCulto.CANCELADO);
    private static final List<StatusEvento> EVENTO_ENCERRADO = List.of(StatusEvento.REALIZADO, StatusEvento.CANCELADO);

    private final IgrejaRepository igrejaRepository;
    private final EscalaRepository escalaRepository;
    private final EscalaVoluntarioRepository escalaVoluntarioRepository;
    private final CultoRepository cultoRepository;
    private final CategoriaCultoRepository categoriaCultoRepository;
    private final CronogramaEventoRepository cronogramaEventoRepository;
    private final CronogramaConteudoRepository cronogramaConteudoRepository;
    private final Clock clock;

    public ParticipacoesFuturasService(
            IgrejaRepository igrejaRepository,
            EscalaRepository escalaRepository,
            EscalaVoluntarioRepository escalaVoluntarioRepository,
            CultoRepository cultoRepository,
            CategoriaCultoRepository categoriaCultoRepository,
            CronogramaEventoRepository cronogramaEventoRepository,
            CronogramaConteudoRepository cronogramaConteudoRepository,
            Clock clock
    ) {
        this.igrejaRepository = igrejaRepository;
        this.escalaRepository = escalaRepository;
        this.escalaVoluntarioRepository = escalaVoluntarioRepository;
        this.cultoRepository = cultoRepository;
        this.categoriaCultoRepository = categoriaCultoRepository;
        this.cronogramaEventoRepository = cronogramaEventoRepository;
        this.cronogramaConteudoRepository = cronogramaConteudoRepository;
        this.clock = clock;
    }

    /** Ajusta as participações futuras da pessoa na igreja que ela está deixando; retorna quantos registros mudaram. */
    @Transactional
    public int ajustarNaSaida(UUID pessoaId, UUID igrejaId) {
        LocalDate hoje = LocalDate.now(clock);
        String mesAtual = YearMonth.from(hoje).toString();
        UUID titular = igrejaRepository.findById(igrejaId).map(Igreja::getPastorPessoaId).orElse(null);
        int ajustadas = 0;

        ajustadas += ajustarDesignacoes(
                escalaVoluntarioRepository.findFuturasDaPessoaNaIgreja(pessoaId, igrejaId, hoje, mesAtual), titular);

        List<Escala> escalas = escalaRepository.findFuturasComSupervisorOuPregador(igrejaId, pessoaId, hoje, mesAtual);
        for (Escala escala : escalas) {
            if (pessoaId.equals(escala.getSupervisorPessoaId())) {
                escala.setSupervisorPessoaId(titular);
            }
            if (pessoaId.equals(escala.getPregadorPessoaId())) {
                escala.setPregadorPessoaId(titular);
            }
        }
        ajustadas += salvar(escalaRepository, escalas);

        List<Culto> cultos = cultoRepository
                .findByIgrejaIdAndPregadorIdAndDataGreaterThanEqualAndStatusNotIn(igrejaId, pessoaId, hoje, CULTO_ENCERRADO);
        cultos.forEach(culto -> culto.setPregadorId(titular));
        ajustadas += salvar(cultoRepository, cultos);

        // Modelo dos próximos cultos (automação mensal) e fallback de culto sem pregador.
        List<CategoriaCulto> categorias = categoriaCultoRepository.findByIgrejaIdAndPregadorPadraoId(igrejaId, pessoaId);
        categorias.forEach(categoria -> categoria.setPregadorPadraoId(titular));
        ajustadas += salvar(categoriaCultoRepository, categorias);

        List<CronogramaEvento> eventos = cronogramaEventoRepository
                .findByIgrejaIdAndSupervisorPessoaIdAndDataEventoGreaterThanEqualAndStatusNotIn(
                        igrejaId, pessoaId, hoje, EVENTO_ENCERRADO);
        eventos.forEach(evento -> evento.setSupervisorPessoaId(titular));
        ajustadas += salvar(cronogramaEventoRepository, eventos);

        List<CronogramaConteudo> conteudos = cronogramaConteudoRepository
                .findFuturosComPregador(igrejaId, pessoaId, hoje, EVENTO_ENCERRADO);
        conteudos.forEach(conteudo -> conteudo.setPregadorPessoaId(titular));
        ajustadas += salvar(cronogramaConteudoRepository, conteudos);

        return ajustadas;
    }

    /**
     * Em escalas de SUPERVISOR/PREGADOR a designação passa ao titular, pendente de confirmação dele — salvo se
     * não houver titular ou ele já estiver designado na mesma escala e dia (ou evento, sem data). As demais
     * (CULTO/MINISTERIO, e as que não passam) são removidas, como em "remover designação": a vaga fica aberta.
     */
    private int ajustarDesignacoes(List<EscalaVoluntario> designacoes, UUID titular) {
        if (designacoes.isEmpty()) {
            return 0;
        }
        Set<UUID> escalaIds = designacoes.stream().map(EscalaVoluntario::getEscalaId).collect(Collectors.toSet());
        Map<UUID, TipoEscala> tipos = new HashMap<>();
        escalaRepository.findAllById(escalaIds).forEach(e -> tipos.put(e.getId(), e.getTipo()));
        Set<List<Object>> ocupadosPeloTitular = new HashSet<>();
        if (titular != null) {
            escalaVoluntarioRepository.findByEscalaIdInAndPessoaId(escalaIds, titular)
                    .forEach(ev -> ocupadosPeloTitular.add(chaveDoDia(ev)));
        }

        List<EscalaVoluntario> reatribuidas = new ArrayList<>();
        List<EscalaVoluntario> removidas = new ArrayList<>();
        for (EscalaVoluntario designacao : designacoes) {
            TipoEscala tipo = tipos.get(designacao.getEscalaId());
            boolean deLideranca = tipo == TipoEscala.SUPERVISOR || tipo == TipoEscala.PREGADOR;
            // add() devolve false se o titular já ocupa aquele dia/evento (inclusive por reatribuição anterior).
            if (deLideranca && titular != null && ocupadosPeloTitular.add(chaveDoDia(designacao))) {
                designacao.setPessoaId(titular);
                designacao.setStatus(StatusEscalaVoluntario.PENDENTE);
                designacao.setConfirmadoPeloVoluntario(false);
                designacao.setObservacoes(null);
                reatribuidas.add(designacao);
            } else {
                removidas.add(designacao);
            }
        }
        if (!removidas.isEmpty()) {
            escalaVoluntarioRepository.deleteAll(removidas);
        }
        return removidas.size() + salvar(escalaVoluntarioRepository, reatribuidas);
    }

    /** Mesma escala e mesmo dia; sem data, mesmo evento. */
    private static List<Object> chaveDoDia(EscalaVoluntario designacao) {
        return Arrays.asList(
                designacao.getEscalaId(),
                designacao.getDataItem() != null ? designacao.getDataItem() : designacao.getEventoId());
    }

    private static <T> int salvar(CrudRepository<T, UUID> repository, List<T> itens) {
        if (itens.isEmpty()) {
            return 0;
        }
        repository.saveAll(itens);
        return itens.size();
    }
}
