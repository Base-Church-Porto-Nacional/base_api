package com.example.base_api.cronograma.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.cronograma.dto.CronogramaEventoCreateRequest;
import com.example.base_api.cronograma.dto.CronogramaEventoResponse;
import com.example.base_api.cronograma.dto.CronogramaEventoUpdateRequest;
import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CronogramaService {

    private final CronogramaEventoRepository repository;

    public CronogramaService(CronogramaEventoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CronogramaEventoResponse> listar(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null) {
            if (fim.isBefore(inicio)) {
                throw new BusinessException("Data fim deve ser igual ou posterior à data início");
            }
            return repository.findByDataEventoBetweenOrderByDataEventoAscHoraInicioAsc(inicio, fim).stream()
                    .map(this::toResponse)
                    .toList();
        }
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CronogramaEventoResponse buscarPorId(UUID id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public CronogramaEventoResponse criar(CronogramaEventoCreateRequest request) {
        validarHorario(request.horaInicio(), request.horaFim());

        CronogramaEvento evento = new CronogramaEvento();
        evento.setTitulo(request.titulo().trim());
        evento.setTipo(request.tipo());
        evento.setDescricao(request.descricao());
        evento.setDataEvento(request.dataEvento());
        evento.setHoraInicio(request.horaInicio());
        evento.setHoraFim(request.horaFim());
        evento.setLocalEvento(request.localEvento());
        evento.setAtivo(true);

        return toResponse(repository.save(evento));
    }

    @Transactional
    public CronogramaEventoResponse atualizar(UUID id, CronogramaEventoUpdateRequest request) {
        CronogramaEvento evento = buscarEntidade(id);
        validarHorario(request.horaInicio(), request.horaFim());

        evento.setTitulo(request.titulo().trim());
        evento.setTipo(request.tipo());
        evento.setDescricao(request.descricao());
        evento.setDataEvento(request.dataEvento());
        evento.setHoraInicio(request.horaInicio());
        evento.setHoraFim(request.horaFim());
        evento.setLocalEvento(request.localEvento());
        evento.setAtivo(request.ativo());

        return toResponse(repository.save(evento));
    }

    @Transactional
    public void excluir(UUID id) {
        CronogramaEvento evento = buscarEntidade(id);
        repository.delete(evento);
    }

    @Transactional(readOnly = true)
    public CronogramaEvento buscarEntidade(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento do cronograma não encontrado"));
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
                evento.getAtivo(),
                evento.getCreatedAt(),
                evento.getUpdatedAt()
        );
    }
}
