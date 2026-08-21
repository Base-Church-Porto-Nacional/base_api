package com.example.base_api.disponibilidade.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.disponibilidade.dto.DisponibilidadeSemanalRequest;
import com.example.base_api.disponibilidade.dto.DisponibilidadeSemanalResponse;
import com.example.base_api.disponibilidade.dto.IndisponibilidadeRequest;
import com.example.base_api.disponibilidade.dto.IndisponibilidadeResponse;
import com.example.base_api.disponibilidade.entity.DisponibilidadeSemanal;
import com.example.base_api.disponibilidade.entity.Indisponibilidade;
import com.example.base_api.disponibilidade.repository.DisponibilidadeSemanalRepository;
import com.example.base_api.disponibilidade.repository.IndisponibilidadeRepository;
import com.example.base_api.pessoa.service.PessoaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DisponibilidadeService {

    private final DisponibilidadeSemanalRepository disponibilidadeSemanalRepository;
    private final IndisponibilidadeRepository indisponibilidadeRepository;
    private final PessoaService pessoaService;

    public DisponibilidadeService(
            DisponibilidadeSemanalRepository disponibilidadeSemanalRepository,
            IndisponibilidadeRepository indisponibilidadeRepository,
            PessoaService pessoaService
    ) {
        this.disponibilidadeSemanalRepository = disponibilidadeSemanalRepository;
        this.indisponibilidadeRepository = indisponibilidadeRepository;
        this.pessoaService = pessoaService;
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadeSemanalResponse> listarSemanal(UUID pessoaId) {
        pessoaService.buscarEntidade(pessoaId);
        return disponibilidadeSemanalRepository.findByPessoaId(pessoaId).stream()
                .map(this::toSemanalResponse)
                .toList();
    }

    @Transactional
    public DisponibilidadeSemanalResponse criarSemanal(DisponibilidadeSemanalRequest request) {
        pessoaService.buscarEntidade(request.pessoaId());
        if (!request.horaFim().isAfter(request.horaInicio())) {
            throw new BusinessException("Hora fim deve ser posterior à hora início");
        }

        DisponibilidadeSemanal disponibilidade = new DisponibilidadeSemanal();
        disponibilidade.setPessoaId(request.pessoaId());
        disponibilidade.setDiaSemana(request.diaSemana());
        disponibilidade.setHoraInicio(request.horaInicio());
        disponibilidade.setHoraFim(request.horaFim());
        disponibilidade.setDisponivel(request.disponivel());
        disponibilidade.setObservacoes(request.observacoes());

        return toSemanalResponse(disponibilidadeSemanalRepository.save(disponibilidade));
    }

    @Transactional
    public void excluirSemanal(UUID id) {
        DisponibilidadeSemanal disponibilidade = disponibilidadeSemanalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilidade semanal não encontrada"));
        disponibilidadeSemanalRepository.delete(disponibilidade);
    }

    @Transactional(readOnly = true)
    public List<IndisponibilidadeResponse> listarIndisponibilidades(UUID pessoaId) {
        pessoaService.buscarEntidade(pessoaId);
        return indisponibilidadeRepository.findByPessoaId(pessoaId).stream()
                .map(this::toIndisponibilidadeResponse)
                .toList();
    }

    @Transactional
    public IndisponibilidadeResponse criarIndisponibilidade(IndisponibilidadeRequest request) {
        pessoaService.buscarEntidade(request.pessoaId());
        if (request.dataFim().isBefore(request.dataInicio())) {
            throw new BusinessException("Data fim deve ser igual ou posterior à data início");
        }

        Indisponibilidade indisponibilidade = new Indisponibilidade();
        indisponibilidade.setPessoaId(request.pessoaId());
        indisponibilidade.setDataInicio(request.dataInicio());
        indisponibilidade.setDataFim(request.dataFim());
        indisponibilidade.setHoraInicio(request.horaInicio());
        indisponibilidade.setHoraFim(request.horaFim());
        indisponibilidade.setMotivo(request.motivo());
        indisponibilidade.setObservacoes(request.observacoes());

        return toIndisponibilidadeResponse(indisponibilidadeRepository.save(indisponibilidade));
    }

    @Transactional
    public void excluirIndisponibilidade(UUID id) {
        Indisponibilidade indisponibilidade = indisponibilidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Indisponibilidade não encontrada"));
        indisponibilidadeRepository.delete(indisponibilidade);
    }

    private DisponibilidadeSemanalResponse toSemanalResponse(DisponibilidadeSemanal entity) {
        return new DisponibilidadeSemanalResponse(
                entity.getId(),
                entity.getPessoaId(),
                entity.getDiaSemana(),
                entity.getHoraInicio(),
                entity.getHoraFim(),
                entity.getDisponivel(),
                entity.getObservacoes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private IndisponibilidadeResponse toIndisponibilidadeResponse(Indisponibilidade entity) {
        return new IndisponibilidadeResponse(
                entity.getId(),
                entity.getPessoaId(),
                entity.getDataInicio(),
                entity.getDataFim(),
                entity.getHoraInicio(),
                entity.getHoraFim(),
                entity.getMotivo(),
                entity.getObservacoes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
