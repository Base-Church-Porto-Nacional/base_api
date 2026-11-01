package com.example.base_api.culto.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.culto.dto.CultoConteudoResponse;
import com.example.base_api.culto.dto.CultoConteudoUpdateRequest;
import com.example.base_api.culto.dto.CultoCreateRequest;
import com.example.base_api.culto.dto.CultoRelatorioConsolidadoResponse;
import com.example.base_api.culto.dto.CultoRelatorioItemResponse;
import com.example.base_api.culto.dto.CultoRelatorioRequest;
import com.example.base_api.culto.dto.CultoRelatorioResponse;
import com.example.base_api.culto.dto.CultoResponse;
import com.example.base_api.culto.dto.CultoUpdateRequest;
import com.example.base_api.culto.entity.CategoriaCulto;
import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.entity.CultoRelatorio;
import com.example.base_api.culto.entity.StatusCulto;
import com.example.base_api.culto.repository.CategoriaCultoRepository;
import com.example.base_api.culto.repository.CultoRelatorioRepository;
import com.example.base_api.culto.repository.CultoRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.base_api.culto.entity.StatusCulto.ABERTO;
import static com.example.base_api.culto.entity.StatusCulto.AGENDADO;
import static com.example.base_api.culto.entity.StatusCulto.CANCELADO;

@Service
public class CultoService {

    private final CultoRepository cultoRepository;
    private final CategoriaCultoRepository categoriaRepository;
    private final CultoRelatorioRepository relatorioRepository;
    private final AutorizacaoService autorizacaoService;
    private final PessoaRepository pessoaRepository;

    public CultoService(
            CultoRepository cultoRepository,
            CategoriaCultoRepository categoriaRepository,
            CultoRelatorioRepository relatorioRepository,
            AutorizacaoService autorizacaoService,
            PessoaRepository pessoaRepository
    ) {
        this.cultoRepository = cultoRepository;
        this.categoriaRepository = categoriaRepository;
        this.relatorioRepository = relatorioRepository;
        this.autorizacaoService = autorizacaoService;
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional(readOnly = true)
    public List<CultoResponse> listar(
            UUID igrejaId,
            UUID categoriaId,
            String competencia,
            StatusCulto status,
            LocalDate inicio,
            LocalDate fim,
            Boolean ativo
    ) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return List.of();
        }
        validarPeriodo(inicio, fim);
        List<Culto> cultos = cultoRepository.filtrar(ids, categoriaId, competencia, status, inicio, fim, ativo);
        if (cultos.isEmpty()) {
            return List.of();
        }
        Set<UUID> comRelatorio = relatorioRepository.findByCultoIdIn(cultos.stream()
                        .map(Culto::getId).toList()).stream()
                .map(CultoRelatorio::getCultoId)
                .collect(Collectors.toSet());
        Map<UUID, CategoriaCulto> categorias = categoriasDos(cultos);
        Map<UUID, String> pregadores = pessoaRepository.nomesPorId(cultos.stream().map(Culto::getPregadorId).toList());
        return cultos.stream().map(c -> toResponse(
                c,
                categorias.get(c.getCategoriaCultoId()),
                comRelatorio.contains(c.getId()),
                c.getPregadorId() == null ? null : pregadores.get(c.getPregadorId())
        )).toList();
    }

    @Transactional(readOnly = true)
    public CultoResponse buscarPorId(UUID id) {
        Culto culto = buscarEntidade(id);
        autorizacaoService.assertAcesso(culto.getIgrejaId());
        return toResponse(
                culto,
                categoriaRepository.findById(culto.getCategoriaCultoId()).orElse(null),
                relatorioRepository.existsByCultoId(id)
        );
    }

    @Transactional
    public CultoResponse criar(CultoCreateRequest request) {
        CategoriaCulto categoria = buscarCategoria(request.categoriaCultoId());
        autorizacaoService.assertPastorOuAdmin(categoria.getIgrejaId());
        validarPregadorDoCulto(request.pregadorId(), categoria.getIgrejaId());

        if (cultoRepository.findByCategoriaCultoIdAndData(categoria.getId(), request.data()).isPresent()) {
            throw new ConflictException("Já existe um culto desta categoria na data informada");
        }

        LocalTime horaInicio = request.horaInicio() != null ? request.horaInicio() : categoria.getHoraInicio();
        LocalTime horaFim = request.horaFim() != null
                ? request.horaFim()
                : horaInicio.plusMinutes(categoria.getDuracaoMinutos());
        validarHorario(horaInicio, horaFim);

        Culto culto = new Culto();
        culto.setIgrejaId(categoria.getIgrejaId());
        culto.setCategoriaCultoId(categoria.getId());
        culto.setTitulo(request.titulo() == null || request.titulo().isBlank()
                ? categoria.getTitulo()
                : request.titulo().trim());
        culto.setTema(request.tema());
        culto.setData(request.data());
        culto.setHoraInicio(horaInicio);
        culto.setHoraFim(horaFim);
        culto.setPregadorId(request.pregadorId() != null ? request.pregadorId() : categoria.getPregadorPadraoId());
        culto.setStatus(AGENDADO);
        culto.setCompetencia(YearMonth.from(request.data()).toString());
        culto.setAtivo(true);

        return toResponse(cultoRepository.save(culto), categoria, false);
    }

    @Transactional
    public CultoResponse atualizar(UUID id, CultoUpdateRequest request) {
        Culto culto = buscarEntidade(id);
        autorizacaoService.assertPastorOuAdmin(culto.getIgrejaId());
        validarTransicaoStatus(culto.getStatus(), request.status());
        validarPregadorDoCulto(request.pregadorId(), culto.getIgrejaId());
        validarHorario(request.horaInicio(), request.horaFim());

        if (!request.data().equals(culto.getData())) {
            cultoRepository.findByCategoriaCultoIdAndData(culto.getCategoriaCultoId(), request.data())
                    .filter(existente -> !existente.getId().equals(id))
                    .ifPresent(existente -> {
                        throw new ConflictException("Já existe um culto desta categoria na data informada");
                    });
        }

        culto.setTitulo(request.titulo().trim());
        culto.setTema(request.tema());
        culto.setData(request.data());
        culto.setHoraInicio(request.horaInicio());
        culto.setHoraFim(request.horaFim());
        culto.setPregadorId(request.pregadorId());
        culto.setStatus(request.status());
        culto.setCompetencia(YearMonth.from(request.data()).toString());

        CategoriaCulto categoria = buscarCategoria(culto.getCategoriaCultoId());
        return toResponse(cultoRepository.save(culto), categoria, relatorioRepository.existsByCultoId(id));
    }

    @Transactional
    public void excluir(UUID id) {
        Culto culto = buscarEntidade(id);
        autorizacaoService.assertPastorOuAdmin(culto.getIgrejaId());
        if (relatorioRepository.existsByCultoId(id)) {
            throw new BusinessException("Não é possível excluir um culto que já possui relatório");
        }
        cultoRepository.delete(culto);
    }

    @Transactional(readOnly = true)
    public CultoConteudoResponse obterConteudo(UUID id) {
        Culto culto = buscarEntidade(id);
        autorizacaoService.assertAcesso(culto.getIgrejaId());
        UUID pregadorId = resolverPregador(culto);
        autorizacaoService.assertPodeVerConteudo(culto.getIgrejaId(), pregadorId);
        return toConteudoResponse(culto, pregadorId);
    }

    @Transactional
    public CultoConteudoResponse salvarConteudo(UUID id, CultoConteudoUpdateRequest request) {
        Culto culto = buscarEntidade(id);
        autorizacaoService.assertAcesso(culto.getIgrejaId());

        UUID pregadorPersistido = resolverPregador(culto);
        boolean pastorAdmin = autorizacaoService.isPastorOuAdmin(culto.getIgrejaId());
        boolean podeTexto = autorizacaoService.podeEditarTexto(culto.getIgrejaId(), pregadorPersistido);
        boolean podeLouvor = autorizacaoService.podeEditarLouvor(culto.getIgrejaId());
        if (!podeTexto && !podeLouvor) {
            throw new AccessDeniedException("Sem permissão para editar o conteúdo deste culto");
        }

        if (podeTexto && request.textoPregacao() != null) {
            culto.setTextoPregacao(request.textoPregacao());
        }
        if (podeLouvor && request.louvor() != null) {
            culto.setLouvor(request.louvor());
        }
        if (request.tema() != null && (pastorAdmin || podeTexto || podeLouvor)) {
            culto.setTema(request.tema());
        }
        if (request.pregadorPessoaId() != null) {
            if (!pastorAdmin) {
                throw new AccessDeniedException("Somente pastor ou admin pode alterar o pregador");
            }
            validarPregadorDoCulto(request.pregadorPessoaId(), culto.getIgrejaId());
            culto.setPregadorId(request.pregadorPessoaId());
        } else if (culto.getPregadorId() == null) {
            CategoriaCulto categoria = buscarCategoria(culto.getCategoriaCultoId());
            culto.setPregadorId(categoria.getPregadorPadraoId());
        }

        return toConteudoResponse(cultoRepository.save(culto), culto.getPregadorId());
    }

    @Transactional(readOnly = true)
    public CultoRelatorioResponse obterRelatorio(UUID id) {
        Culto culto = buscarEntidade(id);
        autorizacaoService.assertAcesso(culto.getIgrejaId());
        return relatorioRepository.findByCultoId(id)
                .map(this::toRelatorioResponse)
                .orElseGet(() -> toRelatorioResponse(CultoRelatorio.vazio(culto.getId())));
    }

    @Transactional
    public CultoRelatorioResponse salvarRelatorio(UUID id, CultoRelatorioRequest request) {
        Culto culto = buscarEntidade(id);
        autorizacaoService.assertPodeLancarContagem(culto.getIgrejaId());
        if (culto.getStatus() == CANCELADO) {
            throw new BusinessException("Não é possível lançar relatório de um culto cancelado");
        }
        validarValoresPositivos(request);

        CultoRelatorio relatorio = relatorioRepository.findByCultoId(id).orElseGet(() -> {
            CultoRelatorio novo = new CultoRelatorio();
            novo.setCultoId(id);
            return novo;
        });
        relatorio.setAdultos(valorOuZero(request.adultos()));
        relatorio.setCriancas(valorOuZero(request.criancas()));
        relatorio.setConversoes(valorOuZero(request.conversoes()));
        relatorio.setReconciliacoes(valorOuZero(request.reconciliacoes()));
        relatorio.setAliancas(valorOuZero(request.aliancas()));
        relatorio.setCarros(valorOuZero(request.carros()));
        relatorio.setMotos(valorOuZero(request.motos()));
        relatorio.setBicicletas(valorOuZero(request.bicicletas()));
        relatorio.setObservacoes(request.observacoes());

        culto.setStatus(StatusCulto.REALIZADO);
        cultoRepository.save(culto);
        return toRelatorioResponse(relatorioRepository.save(relatorio));
    }

    /**
     * Soma a contagem dos cultos do período (competência ou intervalo de datas)
     * nas igrejas visíveis. Cultos cancelados e inativos ficam de fora.
     */
    @Transactional(readOnly = true)
    public CultoRelatorioConsolidadoResponse relatorioConsolidado(
            UUID igrejaId,
            UUID categoriaId,
            String competencia,
            LocalDate inicio,
            LocalDate fim
    ) {
        validarCompetencia(competencia);
        validarPeriodo(inicio, fim);
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        List<Culto> cultos = ids.isEmpty()
                ? List.of()
                : cultoRepository.filtrar(ids, categoriaId, competencia, null, inicio, fim, true).stream()
                        .filter(c -> c.getStatus() != CANCELADO)
                        .toList();

        Map<UUID, CultoRelatorio> relatorios = cultos.isEmpty()
                ? Map.of()
                : relatorioRepository.findByCultoIdIn(cultos.stream().map(Culto::getId).toList()).stream()
                        .collect(Collectors.toMap(CultoRelatorio::getCultoId, Function.identity()));
        Map<UUID, CategoriaCulto> categorias = cultos.isEmpty() ? Map.of() : categoriasDos(cultos);

        int adultos = 0, criancas = 0, conversoes = 0, reconciliacoes = 0, aliancas = 0;
        int carros = 0, motos = 0, bicicletas = 0;
        for (CultoRelatorio r : relatorios.values()) {
            adultos += r.getAdultos();
            criancas += r.getCriancas();
            conversoes += r.getConversoes();
            reconciliacoes += r.getReconciliacoes();
            aliancas += r.getAliancas();
            carros += r.getCarros();
            motos += r.getMotos();
            bicicletas += r.getBicicletas();
        }
        int comRelatorio = relatorios.size();
        int totalPresentes = adultos + criancas;
        int totalVeiculos = carros + motos + bicicletas;

        List<CultoRelatorioItemResponse> itens = cultos.stream().map(c -> {
            CategoriaCulto categoria = categorias.get(c.getCategoriaCultoId());
            CultoRelatorio relatorio = relatorios.get(c.getId());
            return new CultoRelatorioItemResponse(
                    c.getId(),
                    c.getIgrejaId(),
                    c.getCategoriaCultoId(),
                    categoria == null ? null : categoria.getTitulo(),
                    c.getTitulo(),
                    c.getData(),
                    c.getHoraInicio(),
                    c.getStatus(),
                    relatorio != null,
                    relatorio == null ? null : toRelatorioResponse(relatorio)
            );
        }).toList();

        return new CultoRelatorioConsolidadoResponse(
                competencia,
                inicio,
                fim,
                cultos.size(),
                comRelatorio,
                cultos.size() - comRelatorio,
                adultos,
                criancas,
                totalPresentes,
                conversoes,
                reconciliacoes,
                aliancas,
                carros,
                motos,
                bicicletas,
                totalVeiculos,
                media(totalPresentes, comRelatorio),
                media(totalVeiculos, comRelatorio),
                itens
        );
    }

    @Transactional(readOnly = true)
    public Culto buscarEntidade(UUID id) {
        return cultoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Culto não encontrado"));
    }

    private CategoriaCulto buscarCategoria(UUID id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria de culto não encontrada"));
    }

    private UUID resolverPregador(Culto culto) {
        if (culto.getPregadorId() != null) {
            return culto.getPregadorId();
        }
        return buscarCategoria(culto.getCategoriaCultoId()).getPregadorPadraoId();
    }

    private Map<UUID, CategoriaCulto> categoriasDos(List<Culto> cultos) {
        Set<UUID> ids = cultos.stream().map(Culto::getCategoriaCultoId).collect(Collectors.toSet());
        return categoriaRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(CategoriaCulto::getId, Function.identity()));
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new BusinessException("Data fim deve ser igual ou posterior à data início");
        }
    }

    private void validarCompetencia(String competencia) {
        if (competencia == null) {
            return;
        }
        try {
            YearMonth.parse(competencia);
        } catch (DateTimeParseException e) {
            throw new BusinessException("Competência inválida. Use o formato YYYY-MM");
        }
    }

    private Double media(int total, int quantidade) {
        if (quantidade == 0) {
            return 0.0;
        }
        return Math.round(total * 10.0 / quantidade) / 10.0;
    }

    private void validarHorario(LocalTime inicio, LocalTime fim) {
        if (fim != null && !fim.isAfter(inicio)) {
            throw new BusinessException("Hora fim deve ser posterior à hora início");
        }
    }

    private void validarPregadorDoCulto(UUID pregadorId, UUID igrejaId) {
        if (pregadorId == null) {
            return;
        }
        Pessoa pessoa = pessoaRepository.findById(pregadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada"));
        autorizacaoService.assertPessoaDaIgreja(pessoa, igrejaId);
    }

    private void validarTransicaoStatus(StatusCulto atual, StatusCulto novo) {
        if (atual == novo) {
            return;
        }
        boolean valida = switch (atual) {
            case AGENDADO -> novo == ABERTO || novo == CANCELADO;
            case ABERTO -> novo == AGENDADO || novo == StatusCulto.REALIZADO || novo == CANCELADO;
            case REALIZADO, CANCELADO -> false;
        };
        if (!valida) {
            throw new BusinessException("Transição de status inválida: " + atual + " → " + novo);
        }
    }

    private void validarValoresPositivos(CultoRelatorioRequest request) {
        int[] valores = {
                request.adultos() == null ? 0 : request.adultos(),
                request.criancas() == null ? 0 : request.criancas(),
                request.conversoes() == null ? 0 : request.conversoes(),
                request.reconciliacoes() == null ? 0 : request.reconciliacoes(),
                request.aliancas() == null ? 0 : request.aliancas(),
                request.carros() == null ? 0 : request.carros(),
                request.motos() == null ? 0 : request.motos(),
                request.bicicletas() == null ? 0 : request.bicicletas()
        };
        for (int valor : valores) {
            if (valor < 0) {
                throw new BusinessException("Os valores do relatório não podem ser negativos");
            }
        }
    }

    private Integer valorOuZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private CultoConteudoResponse toConteudoResponse(Culto culto, UUID pregadorId) {
        return new CultoConteudoResponse(
                culto.getId(),
                culto.getTextoPregacao() == null ? "" : culto.getTextoPregacao(),
                culto.getLouvor() == null ? "" : culto.getLouvor(),
                culto.getTema(),
                pregadorId,
                nomePessoa(pregadorId)
        );
    }

    private CultoRelatorioResponse toRelatorioResponse(CultoRelatorio relatorio) {
        int adultos = relatorio.getAdultos();
        int criancas = relatorio.getCriancas();
        int carros = relatorio.getCarros();
        int motos = relatorio.getMotos();
        int bicicletas = relatorio.getBicicletas();
        return new CultoRelatorioResponse(
                relatorio.getCultoId(),
                adultos,
                criancas,
                adultos + criancas,
                relatorio.getConversoes(),
                relatorio.getReconciliacoes(),
                relatorio.getAliancas(),
                carros,
                motos,
                bicicletas,
                carros + motos + bicicletas,
                relatorio.getObservacoes(),
                relatorio.getCreatedAt(),
                relatorio.getUpdatedAt()
        );
    }

    private CultoResponse toResponse(Culto culto, CategoriaCulto categoria, boolean temRelatorio) {
        return toResponse(culto, categoria, temRelatorio, nomePessoa(culto.getPregadorId()));
    }

    private CultoResponse toResponse(
            Culto culto,
            CategoriaCulto categoria,
            boolean temRelatorio,
            String pregadorNome
    ) {
        return new CultoResponse(
                culto.getId(),
                culto.getIgrejaId(),
                culto.getCategoriaCultoId(),
                categoria == null ? null : categoria.getTitulo(),
                culto.getTitulo(),
                culto.getTema(),
                culto.getData(),
                culto.getHoraInicio(),
                culto.getHoraFim(),
                culto.getPregadorId(),
                pregadorNome,
                culto.getStatus(),
                culto.getCompetencia(),
                culto.getEscalaId(),
                culto.getAtivo(),
                janelaConfiguracaoAberta(culto),
                temRelatorio,
                culto.getCreatedAt(),
                culto.getUpdatedAt()
        );
    }

    private boolean janelaConfiguracaoAberta(Culto culto) {
        return !culto.getData().isAfter(LocalDate.now().plusDays(8));
    }

    private String nomePessoa(UUID pessoaId) {
        if (pessoaId == null) {
            return null;
        }
        return pessoaRepository.findById(pessoaId).map(Pessoa::getNome).orElse(null);
    }
}