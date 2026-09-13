package com.example.base_api.cronograma.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.cronograma.dto.CronogramaConteudoResponse;
import com.example.base_api.cronograma.dto.CronogramaConteudoUpdateRequest;
import com.example.base_api.cronograma.dto.CronogramaEventoCreateRequest;
import com.example.base_api.cronograma.dto.CronogramaEventoResponse;
import com.example.base_api.cronograma.dto.CronogramaEventoUpdateRequest;
import com.example.base_api.cronograma.entity.CronogramaConteudo;
import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.entity.EventoMinisterio;
import com.example.base_api.cronograma.entity.StatusEvento;
import com.example.base_api.cronograma.repository.CronogramaConteudoRepository;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.cronograma.repository.EventoMinisterioRepository;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaServico;
import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.entity.StatusEscala;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.escala.service.GeracaoEscalaService;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.service.IgrejaContextoService;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CronogramaService {

    private final CronogramaEventoRepository repository;
    private final CronogramaConteudoRepository conteudoRepository;
    private final EventoMinisterioRepository eventoMinisterioRepository;
    private final IgrejaContextoService igrejaContextoService;
    private final AutorizacaoService autorizacaoService;
    private final PessoaRepository pessoaRepository;
    private final MinisterioRepository ministerioRepository;
    private final ServicoRepository servicoRepository;
    private final PessoaServicoRepository pessoaServicoRepository;
    private final EscalaRepository escalaRepository;
    private final EscalaServicoRepository escalaServicoRepository;
    private final EscalaVoluntarioRepository escalaVoluntarioRepository;
    private final GeracaoEscalaService geracaoEscalaService;

    public CronogramaService(
            CronogramaEventoRepository repository,
            CronogramaConteudoRepository conteudoRepository,
            EventoMinisterioRepository eventoMinisterioRepository,
            IgrejaContextoService igrejaContextoService,
            AutorizacaoService autorizacaoService,
            PessoaRepository pessoaRepository,
            MinisterioRepository ministerioRepository,
            ServicoRepository servicoRepository,
            PessoaServicoRepository pessoaServicoRepository,
            EscalaRepository escalaRepository,
            EscalaServicoRepository escalaServicoRepository,
            EscalaVoluntarioRepository escalaVoluntarioRepository,
            GeracaoEscalaService geracaoEscalaService
    ) {
        this.repository = repository;
        this.conteudoRepository = conteudoRepository;
        this.eventoMinisterioRepository = eventoMinisterioRepository;
        this.igrejaContextoService = igrejaContextoService;
        this.autorizacaoService = autorizacaoService;
        this.pessoaRepository = pessoaRepository;
        this.ministerioRepository = ministerioRepository;
        this.servicoRepository = servicoRepository;
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.escalaRepository = escalaRepository;
        this.escalaServicoRepository = escalaServicoRepository;
        this.escalaVoluntarioRepository = escalaVoluntarioRepository;
        this.geracaoEscalaService = geracaoEscalaService;
    }

    @Transactional(readOnly = true)
    public List<CronogramaEventoResponse> listar(UUID igrejaId, LocalDate inicio, LocalDate fim) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new BusinessException("Data fim deve ser igual ou posterior à data início");
        }
        if (ids.isEmpty()) {
            return List.of();
        }

        List<CronogramaEvento> eventos;
        if (inicio != null && fim != null) {
            eventos = repository.findByIgrejaIdInAndDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(
                    ids, inicio, fim
            );
        } else {
            eventos = repository.findByIgrejaIdInOrderByDataEventoAscHoraInicioAsc(ids);
        }
        return eventos.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CronogramaEventoResponse buscarPorId(UUID id) {
        CronogramaEvento evento = buscarEntidade(id);
        autorizacaoService.assertAcesso(evento.getIgrejaId());
        return toResponse(evento);
    }

    @Transactional
    public CronogramaEventoResponse criar(CronogramaEventoCreateRequest request) {
        validarHorario(request.horaInicio(), request.horaFim());
        UUID igrejaId = request.igrejaId() != null
                ? request.igrejaId()
                : autorizacaoService.requireUsuario().getIgrejaId();
        if (igrejaId == null) {
            throw new BusinessException("igrejaId é obrigatório");
        }
        autorizacaoService.assertPastorOuAdmin(igrejaId);

        Igreja igreja = igrejaContextoService.buscarIgreja(igrejaId);
        UUID supervisorId = request.supervisorPessoaId() != null
                ? request.supervisorPessoaId()
                : igreja.getPastorPessoaId();

        CronogramaEvento evento = new CronogramaEvento();
        evento.setTitulo(request.titulo().trim());
        evento.setTema(request.tema());
        evento.setTipo(request.tipo());
        evento.setDescricao(request.descricao());
        evento.setDataEvento(request.dataEvento());
        evento.setHoraInicio(request.horaInicio());
        evento.setHoraFim(request.horaFim());
        evento.setLocalEvento(request.localEvento());
        evento.setIgrejaId(igrejaId);
        evento.setCompetencia(YearMonth.from(request.dataEvento()).toString());
        evento.setStatus(StatusEvento.AGENDADO);
        evento.setSupervisorPessoaId(supervisorId);
        evento.setAtivo(true);

        CronogramaEvento salvo = repository.save(evento);

        if (request.ministerioIds() != null && !request.ministerioIds().isEmpty()) {
            vincularMinisterios(salvo, igreja, request.ministerioIds());
        }

        return toResponse(salvo);
    }

    @Transactional
    public CronogramaEventoResponse atualizar(UUID id, CronogramaEventoUpdateRequest request) {
        CronogramaEvento evento = buscarEntidade(id);
        autorizacaoService.assertPastorOuAdmin(evento.getIgrejaId());
        validarHorario(request.horaInicio(), request.horaFim());
        validarTransicaoStatus(evento.getStatus(), request.status());

        evento.setTitulo(request.titulo().trim());
        evento.setTema(request.tema());
        evento.setTipo(request.tipo());
        evento.setDescricao(request.descricao());
        evento.setDataEvento(request.dataEvento());
        evento.setHoraInicio(request.horaInicio());
        evento.setHoraFim(request.horaFim());
        evento.setLocalEvento(request.localEvento());
        if (request.supervisorPessoaId() != null) {
            evento.setSupervisorPessoaId(request.supervisorPessoaId());
        }
        evento.setCompetencia(YearMonth.from(request.dataEvento()).toString());
        evento.setStatus(request.status());
        evento.setAtivo(request.ativo());

        CronogramaEvento salvo = repository.save(evento);

        if (request.ministerioIds() != null) {
            Igreja igreja = igrejaContextoService.buscarIgreja(salvo.getIgrejaId());
            List<UUID> manter = new ArrayList<>(request.ministerioIds());
            List<EventoMinisterio> atuais = eventoMinisterioRepository.findByEventoId(salvo.getId());
            for (EventoMinisterio vinculo : atuais) {
                if (manter.contains(vinculo.getMinisterioId())) {
                    continue;
                }
                eventoMinisterioRepository.delete(vinculo);
                escalaRepository.findByEventoIdAndMinisterioId(salvo.getId(), vinculo.getMinisterioId())
                        .ifPresent(escala -> {
                            escala.setAtivo(false);
                            escalaRepository.save(escala);
                        });
            }
            vincularMinisterios(salvo, igreja, manter);
        }

        return toResponse(salvo);
    }

    @Transactional
    public void excluir(UUID id) {
        CronogramaEvento evento = buscarEntidade(id);
        autorizacaoService.assertPastorOuAdmin(evento.getIgrejaId());
        repository.delete(evento);
    }

    @Transactional(readOnly = true)
    public CronogramaConteudoResponse obterConteudo(UUID eventoId) {
        CronogramaEvento evento = buscarEntidade(eventoId);
        autorizacaoService.assertAcesso(evento.getIgrejaId());
        CronogramaConteudo conteudo = conteudoRepository.findByEventoId(eventoId).orElse(null);
        UUID pregadorId = conteudo != null && conteudo.getPregadorPessoaId() != null
                ? conteudo.getPregadorPessoaId()
                : resolverPregadorDoEvento(evento);
        autorizacaoService.assertPodeVerConteudo(evento.getIgrejaId(), pregadorId);
        return new CronogramaConteudoResponse(
                eventoId,
                conteudo == null || conteudo.getTextoPregacao() == null ? "" : conteudo.getTextoPregacao(),
                conteudo == null || conteudo.getLouvor() == null ? "" : conteudo.getLouvor(),
                pregadorId,
                nomePessoa(pregadorId)
        );
    }

    @Transactional
    public CronogramaConteudoResponse salvarConteudo(UUID eventoId, CronogramaConteudoUpdateRequest request) {
        CronogramaEvento evento = buscarEntidade(eventoId);
        autorizacaoService.assertAcesso(evento.getIgrejaId());

        CronogramaConteudo conteudo = conteudoRepository.findByEventoId(eventoId).orElseGet(() -> {
            CronogramaConteudo novo = new CronogramaConteudo();
            novo.setEventoId(eventoId);
            novo.setTextoPregacao("");
            novo.setLouvor("");
            return novo;
        });

        UUID pregadorPersistido = conteudo.getPregadorPessoaId() != null
                ? conteudo.getPregadorPessoaId()
                : resolverPregadorDoEvento(evento);

        boolean pastorAdmin = autorizacaoService.isPastorOuAdmin(evento.getIgrejaId());
        boolean podeTexto = autorizacaoService.podeEditarTexto(evento.getIgrejaId(), pregadorPersistido);
        boolean podeLouvor = autorizacaoService.podeEditarLouvor(evento.getIgrejaId());
        if (!podeTexto && !podeLouvor) {
            throw new AccessDeniedException("Sem permissão para editar o conteúdo deste culto");
        }

        if (podeTexto && request.textoPregacao() != null) {
            conteudo.setTextoPregacao(request.textoPregacao());
        }
        if (podeLouvor && request.louvor() != null) {
            conteudo.setLouvor(request.louvor());
        }
        if (request.pregadorPessoaId() != null) {
            if (!pastorAdmin) {
                throw new AccessDeniedException("Somente pastor ou admin pode alterar o pregador");
            }
            Pessoa pregador = pessoaRepository.findById(request.pregadorPessoaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada"));
            autorizacaoService.assertPessoaDaIgreja(pregador, evento.getIgrejaId());
            conteudo.setPregadorPessoaId(request.pregadorPessoaId());
        } else if (conteudo.getPregadorPessoaId() == null) {
            conteudo.setPregadorPessoaId(pregadorPersistido);
        }

        CronogramaConteudo salvo = conteudoRepository.save(conteudo);
        return new CronogramaConteudoResponse(
                eventoId,
                salvo.getTextoPregacao() == null ? "" : salvo.getTextoPregacao(),
                salvo.getLouvor() == null ? "" : salvo.getLouvor(),
                salvo.getPregadorPessoaId(),
                nomePessoa(salvo.getPregadorPessoaId())
        );
    }

    @Transactional(readOnly = true)
    public CronogramaEvento buscarEntidade(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento do cronograma não encontrado"));
    }

    private void vincularMinisterios(CronogramaEvento evento, Igreja igreja, List<UUID> ministerioIds) {
        for (UUID ministerioId : ministerioIds) {
            Ministerio ministerio = garantirMinisterioDaIgreja(ministerioId, evento.getIgrejaId());
            boolean jaVinculado = eventoMinisterioRepository
                    .findByEventoIdAndMinisterioId(evento.getId(), ministerioId)
                    .isPresent();
            if (!jaVinculado) {
                EventoMinisterio vinculo = new EventoMinisterio();
                vinculo.setEventoId(evento.getId());
                vinculo.setMinisterioId(ministerioId);
                eventoMinisterioRepository.save(vinculo);
            }
            gerarEscalaDoEvento(evento, igreja, ministerio);
        }
    }

    private void gerarEscalaDoEvento(CronogramaEvento evento, Igreja igreja, Ministerio ministerio) {
        if (escalaRepository.findByEventoIdAndMinisterioId(evento.getId(), ministerio.getId()).isPresent()) {
            return;
        }
        List<Servico> servicos = servicoRepository.findByMinisterioIdOrderByNomeAsc(ministerio.getId())
                .stream()
                .filter(s -> Boolean.TRUE.equals(s.getAtivo()))
                .filter(s -> !Boolean.TRUE.equals(s.getFiccao()))
                .filter(this::temVoluntario)
                .toList();
        if (servicos.isEmpty()) {
            return;
        }

        Escala escala = new Escala();
        escala.setTitulo(tituloEscalaDoEvento(ministerio, evento));
        escala.setDescricao("Sugestão de escala do evento \"" + evento.getTitulo() + "\"");
        escala.setDataEvento(evento.getDataEvento());
        escala.setHoraInicio(evento.getHoraInicio());
        escala.setHoraFim(evento.getHoraFim());
        escala.setLocalEvento(evento.getLocalEvento());
        escala.setEventoId(evento.getId());
        escala.setIgrejaId(evento.getIgrejaId());
        escala.setCompetencia(evento.getCompetencia());
        escala.setTipo(TipoEscala.MINISTERIO);
        escala.setMinisterioId(ministerio.getId());
        escala.setSupervisorPessoaId(evento.getSupervisorPessoaId());
        escala.setPregadorPessoaId(igreja.getPastorPessoaId());
        escala.setAtivo(true);
        escala.setStatus(StatusEscala.SUGERIDA);

        Escala salva = escalaRepository.save(escala);
        for (Servico servico : servicos) {
            EscalaServico necessidade = new EscalaServico();
            necessidade.setEscalaId(salva.getId());
            necessidade.setServicoId(servico.getId());
            necessidade.setQuantidadeNecessaria(1);
            escalaServicoRepository.save(necessidade);
        }
        geracaoEscalaService.gerarParaEvento(salva, evento);
    }

    private String tituloEscalaDoEvento(Ministerio ministerio, CronogramaEvento evento) {
        String titulo = ministerio.getNome() + " — " + evento.getTitulo();
        return titulo.length() <= 200 ? titulo : titulo.substring(0, 200);
    }

    private boolean temVoluntario(Servico servico) {
        return !pessoaServicoRepository.findByServicoIdAndAtivoTrue(servico.getId()).isEmpty();
    }

    private Ministerio garantirMinisterioDaIgreja(UUID ministerioId, UUID igrejaId) {
        Ministerio ministerio = ministerioRepository.findById(ministerioId)
                .orElseThrow(() -> new ResourceNotFoundException("Ministério não encontrado"));
        if (!ministerio.getIgrejaId().equals(igrejaId)) {
            throw new BusinessException("O ministério não pertence a esta igreja");
        }
        return ministerio;
    }

    private UUID resolverPregadorDoEvento(CronogramaEvento evento) {
        String competencia = YearMonth.from(evento.getDataEvento()).toString();
        List<Escala> escalas = escalaRepository.findByIgrejaIdAndCompetenciaAndTipo(
                evento.getIgrejaId(), competencia, TipoEscala.PREGADOR
        );
        for (Escala escala : escalas) {
            for (EscalaVoluntario item : escalaVoluntarioRepository.findByEscalaId(escala.getId())) {
                boolean mesmoEvento = evento.getId().equals(item.getEventoId());
                boolean mesmaData = evento.getDataEvento().equals(item.getDataItem());
                if (mesmoEvento || mesmaData) {
                    return item.getPessoaId();
                }
            }
            if (escala.getPregadorPessoaId() != null) {
                return escala.getPregadorPessoaId();
            }
        }
        Igreja igreja = igrejaContextoService.buscarIgreja(evento.getIgrejaId());
        return igreja.getPastorPessoaId();
    }

    private void validarHorario(java.time.LocalTime inicio, java.time.LocalTime fim) {
        if (fim != null && !fim.isAfter(inicio)) {
            throw new BusinessException("Hora fim deve ser posterior à hora início");
        }
    }

    private void validarTransicaoStatus(StatusEvento atual, StatusEvento novo) {
        if (atual == novo) {
            return;
        }
        boolean valida = switch (atual) {
            case AGENDADO -> novo == StatusEvento.ABERTO || novo == StatusEvento.CANCELADO;
            case ABERTO -> novo == StatusEvento.AGENDADO
                    || novo == StatusEvento.REALIZADO
                    || novo == StatusEvento.CANCELADO;
            case REALIZADO, CANCELADO -> false;
        };
        if (!valida) {
            throw new BusinessException("Transição de status inválida: " + atual + " → " + novo);
        }
    }

    private CronogramaEventoResponse toResponse(CronogramaEvento evento) {
        List<CronogramaEventoResponse.MinisterioParticipante> ministerios = eventoMinisterioRepository
                .findByEventoId(evento.getId())
                .stream()
                .map(vinculo -> new CronogramaEventoResponse.MinisterioParticipante(
                        vinculo.getMinisterioId(),
                        ministerioRepository.findById(vinculo.getMinisterioId())
                                .map(Ministerio::getNome)
                                .orElse(null)))
                .toList();

        List<CronogramaEventoResponse.EscalaEventoResumo> escalas = escalaRepository
                .findByEventoId(evento.getId())
                .stream()
                .map(this::toEscalaResumo)
                .toList();

        return new CronogramaEventoResponse(
                evento.getId(),
                evento.getTitulo(),
                evento.getTema(),
                evento.getTipo(),
                evento.getDescricao(),
                evento.getDataEvento(),
                evento.getHoraInicio(),
                evento.getHoraFim(),
                evento.getLocalEvento(),
                evento.getIgrejaId(),
                evento.getCompetencia(),
                evento.getStatus(),
                evento.getSupervisorPessoaId(),
                nomePessoa(evento.getSupervisorPessoaId()),
                evento.getAtivo(),
                ministerios,
                escalas,
                evento.getCreatedAt(),
                evento.getUpdatedAt()
        );
    }

    private CronogramaEventoResponse.EscalaEventoResumo toEscalaResumo(Escala escala) {
        int vagasTotais = escalaServicoRepository.findByEscalaId(escala.getId())
                .stream()
                .mapToInt(EscalaServico::getQuantidadeNecessaria)
                .sum();
        int vagasPreenchidas = escalaVoluntarioRepository.findByEscalaId(escala.getId()).size();
        String ministerioNome = escala.getMinisterioId() == null
                ? null
                : ministerioRepository.findById(escala.getMinisterioId())
                .map(Ministerio::getNome)
                .orElse(null);
        return new CronogramaEventoResponse.EscalaEventoResumo(
                escala.getId(),
                escala.getMinisterioId(),
                ministerioNome,
                escala.getTitulo(),
                vagasTotais,
                vagasPreenchidas
        );
    }

    private String nomePessoa(UUID pessoaId) {
        if (pessoaId == null) {
            return null;
        }
        return pessoaRepository.findById(pessoaId).map(Pessoa::getNome).orElse(null);
    }
}