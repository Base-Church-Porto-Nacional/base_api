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
import com.example.base_api.cronograma.repository.CronogramaConteudoRepository;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.service.IgrejaContextoService;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class CronogramaService {

    private final CronogramaEventoRepository repository;
    private final CronogramaConteudoRepository conteudoRepository;
    private final IgrejaContextoService igrejaContextoService;
    private final AutorizacaoService autorizacaoService;
    private final PessoaRepository pessoaRepository;
    private final EscalaRepository escalaRepository;
    private final EscalaVoluntarioRepository escalaVoluntarioRepository;

    public CronogramaService(
            CronogramaEventoRepository repository,
            CronogramaConteudoRepository conteudoRepository,
            IgrejaContextoService igrejaContextoService,
            AutorizacaoService autorizacaoService,
            PessoaRepository pessoaRepository,
            EscalaRepository escalaRepository,
            EscalaVoluntarioRepository escalaVoluntarioRepository
    ) {
        this.repository = repository;
        this.conteudoRepository = conteudoRepository;
        this.igrejaContextoService = igrejaContextoService;
        this.autorizacaoService = autorizacaoService;
        this.pessoaRepository = pessoaRepository;
        this.escalaRepository = escalaRepository;
        this.escalaVoluntarioRepository = escalaVoluntarioRepository;
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
        evento.setTipo(request.tipo());
        evento.setDescricao(request.descricao());
        evento.setDataEvento(request.dataEvento());
        evento.setHoraInicio(request.horaInicio());
        evento.setHoraFim(request.horaFim());
        evento.setLocalEvento(request.localEvento());
        evento.setIgrejaId(igrejaId);
        evento.setSupervisorPessoaId(supervisorId);
        evento.setAtivo(true);

        return toResponse(repository.save(evento));
    }

    @Transactional
    public CronogramaEventoResponse atualizar(UUID id, CronogramaEventoUpdateRequest request) {
        CronogramaEvento evento = buscarEntidade(id);
        autorizacaoService.assertPastorOuAdmin(evento.getIgrejaId());
        validarHorario(request.horaInicio(), request.horaFim());

        evento.setTitulo(request.titulo().trim());
        evento.setTipo(request.tipo());
        evento.setDescricao(request.descricao());
        evento.setDataEvento(request.dataEvento());
        evento.setHoraInicio(request.horaInicio());
        evento.setHoraFim(request.horaFim());
        evento.setLocalEvento(request.localEvento());
        if (request.supervisorPessoaId() != null) {
            evento.setSupervisorPessoaId(request.supervisorPessoaId());
        }
        evento.setAtivo(request.ativo());

        return toResponse(repository.save(evento));
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

    private CronogramaEventoResponse toResponse(CronogramaEvento evento) {
        return new CronogramaEventoResponse(
                evento.getId(),
                evento.getTitulo(),
                evento.getTipo(),
                evento.getDescricao(),
                evento.getDataEvento(),
                evento.getHoraInicio(),
                evento.getHoraFim(),
                evento.getLocalEvento(),
                evento.getIgrejaId(),
                evento.getSupervisorPessoaId(),
                nomePessoa(evento.getSupervisorPessoaId()),
                evento.getAtivo(),
                evento.getCreatedAt(),
                evento.getUpdatedAt()
        );
    }

    private String nomePessoa(UUID pessoaId) {
        if (pessoaId == null) {
            return null;
        }
        return pessoaRepository.findById(pessoaId).map(Pessoa::getNome).orElse(null);
    }
}
