package com.example.base_api.pessoa.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.pessoa.dto.PessoaPausaCreateRequest;
import com.example.base_api.pessoa.dto.PessoaPausaResponse;
import com.example.base_api.pessoa.dto.PessoaPausaUpdateRequest;
import com.example.base_api.pessoa.entity.PessoaPausa;
import com.example.base_api.pessoa.repository.PessoaPausaRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PessoaPausaService {

    private final PessoaPausaRepository pausaRepository;
    private final PessoaRepository pessoaRepository;

    public PessoaPausaService(PessoaPausaRepository pausaRepository, PessoaRepository pessoaRepository) {
        this.pausaRepository = pausaRepository;
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional(readOnly = true)
    public List<PessoaPausaResponse> listarPorPessoa(UUID pessoaId) {
        garantirPessoaExiste(pessoaId);
        return pausaRepository.findByPessoaIdOrderByDataInicioDesc(pessoaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PessoaPausaResponse criar(UUID pessoaId, PessoaPausaCreateRequest request) {
        garantirPessoaExiste(pessoaId);
        validarPeriodo(request.dataInicio(), request.dataFim());

        PessoaPausa pausa = new PessoaPausa();
        pausa.setPessoaId(pessoaId);
        pausa.setDataInicio(request.dataInicio());
        pausa.setDataFim(request.dataFim());
        pausa.setMotivo(blankToNull(request.motivo()));
        pausa.setObservacoes(request.observacoes());
        pausa.setAtiva(true);

        return toResponse(pausaRepository.save(pausa));
    }

    @Transactional
    public PessoaPausaResponse atualizar(UUID pessoaId, UUID pausaId, PessoaPausaUpdateRequest request) {
        PessoaPausa pausa = buscarEntidade(pessoaId, pausaId);
        validarPeriodo(request.dataInicio(), request.dataFim());

        pausa.setDataInicio(request.dataInicio());
        pausa.setDataFim(request.dataFim());
        pausa.setMotivo(blankToNull(request.motivo()));
        pausa.setObservacoes(request.observacoes());
        pausa.setAtiva(request.ativa());

        return toResponse(pausaRepository.save(pausa));
    }

    @Transactional
    public void excluir(UUID pessoaId, UUID pausaId) {
        PessoaPausa pausa = buscarEntidade(pessoaId, pausaId);
        pausaRepository.delete(pausa);
    }

    private PessoaPausa buscarEntidade(UUID pessoaId, UUID pausaId) {
        garantirPessoaExiste(pessoaId);
        PessoaPausa pausa = pausaRepository.findById(pausaId)
                .orElseThrow(() -> new ResourceNotFoundException("Pausa não encontrada"));
        if (!pausa.getPessoaId().equals(pessoaId)) {
            throw new ResourceNotFoundException("Pausa não encontrada para esta pessoa");
        }
        return pausa;
    }

    private void garantirPessoaExiste(UUID pessoaId) {
        if (!pessoaRepository.existsById(pessoaId)) {
            throw new ResourceNotFoundException("Pessoa não encontrada");
        }
    }

    private void validarPeriodo(java.time.LocalDate inicio, java.time.LocalDate fim) {
        if (fim != null && fim.isBefore(inicio)) {
            throw new BusinessException("Data fim deve ser igual ou posterior à data início");
        }
    }

    private PessoaPausaResponse toResponse(PessoaPausa pausa) {
        return new PessoaPausaResponse(
                pausa.getId(),
                pausa.getPessoaId(),
                pausa.getDataInicio(),
                pausa.getDataFim(),
                pausa.getMotivo(),
                pausa.getObservacoes(),
                pausa.getAtiva(),
                pausa.getCreatedAt(),
                pausa.getUpdatedAt()
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
