package com.example.base_api.disponibilidade.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.common.util.Horarios;
import com.example.base_api.disponibilidade.dto.DisponibilidadeSemanalRequest;
import com.example.base_api.disponibilidade.dto.DisponibilidadeSemanalResponse;
import com.example.base_api.disponibilidade.dto.IndisponibilidadeRequest;
import com.example.base_api.disponibilidade.dto.IndisponibilidadeResponse;
import com.example.base_api.disponibilidade.entity.DisponibilidadeSemanal;
import com.example.base_api.disponibilidade.entity.Indisponibilidade;
import com.example.base_api.disponibilidade.repository.DisponibilidadeSemanalRepository;
import com.example.base_api.disponibilidade.repository.IndisponibilidadeRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.AutorizacaoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DisponibilidadeService {

    private final DisponibilidadeSemanalRepository disponibilidadeSemanalRepository;
    private final IndisponibilidadeRepository indisponibilidadeRepository;
    private final PessoaService pessoaService;
    private final AutorizacaoService autorizacaoService;

    public DisponibilidadeService(
            DisponibilidadeSemanalRepository disponibilidadeSemanalRepository,
            IndisponibilidadeRepository indisponibilidadeRepository,
            PessoaService pessoaService,
            AutorizacaoService autorizacaoService
    ) {
        this.disponibilidadeSemanalRepository = disponibilidadeSemanalRepository;
        this.indisponibilidadeRepository = indisponibilidadeRepository;
        this.pessoaService = pessoaService;
        this.autorizacaoService = autorizacaoService;
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadeSemanalResponse> listarSemanal(UUID pessoaId) {
        assertAcessoPessoa(pessoaId);
        return disponibilidadeSemanalRepository.findByPessoaId(pessoaId).stream()
                .map(this::toSemanalResponse)
                .toList();
    }

    @Transactional
    public DisponibilidadeSemanalResponse criarSemanal(DisponibilidadeSemanalRequest request) {
        assertAcessoPessoa(request.pessoaId());
        Horarios.validarFimAposInicio(request.horaInicio(), request.horaFim());

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
    public DisponibilidadeSemanalResponse atualizarSemanal(UUID id, DisponibilidadeSemanalRequest request) {
        DisponibilidadeSemanal disponibilidade = disponibilidadeSemanalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilidade semanal não encontrada"));

        assertAcessoPessoa(disponibilidade.getPessoaId());
        assertAcessoPessoa(request.pessoaId());
        Horarios.validarFimAposInicio(request.horaInicio(), request.horaFim());

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
        assertAcessoPessoa(disponibilidade.getPessoaId());
        disponibilidadeSemanalRepository.delete(disponibilidade);
    }

    @Transactional(readOnly = true)
    public List<IndisponibilidadeResponse> listarIndisponibilidades(UUID pessoaId) {
        assertAcessoPessoa(pessoaId);
        return indisponibilidadeRepository.findByPessoaId(pessoaId).stream()
                .map(this::toIndisponibilidadeResponse)
                .toList();
    }

    @Transactional
    public IndisponibilidadeResponse criarIndisponibilidade(IndisponibilidadeRequest request) {
        assertAcessoPessoa(request.pessoaId());
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
    public IndisponibilidadeResponse atualizarIndisponibilidade(UUID id, IndisponibilidadeRequest request) {
        Indisponibilidade indisponibilidade = indisponibilidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Indisponibilidade não encontrada"));

        assertAcessoPessoa(indisponibilidade.getPessoaId());
        assertAcessoPessoa(request.pessoaId());
        if (request.dataFim().isBefore(request.dataInicio())) {
            throw new BusinessException("Data fim deve ser igual ou posterior à data início");
        }

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
        assertAcessoPessoa(indisponibilidade.getPessoaId());
        indisponibilidadeRepository.delete(indisponibilidade);
    }

    private void assertAcessoPessoa(UUID pessoaId) {
        Pessoa pessoa = pessoaService.buscarEntidade(pessoaId);
        autorizacaoService.assertPodeAcessarDisponibilidade(pessoa);
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
