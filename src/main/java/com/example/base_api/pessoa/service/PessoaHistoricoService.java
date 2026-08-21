package com.example.base_api.pessoa.service;

import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.pessoa.dto.PessoaHistoricoCreateRequest;
import com.example.base_api.pessoa.dto.PessoaHistoricoResponse;
import com.example.base_api.pessoa.dto.PessoaHistoricoUpdateRequest;
import com.example.base_api.pessoa.entity.PessoaHistorico;
import com.example.base_api.pessoa.repository.PessoaHistoricoRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PessoaHistoricoService {

    private final PessoaHistoricoRepository historicoRepository;
    private final PessoaRepository pessoaRepository;

    public PessoaHistoricoService(
            PessoaHistoricoRepository historicoRepository,
            PessoaRepository pessoaRepository
    ) {
        this.historicoRepository = historicoRepository;
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional(readOnly = true)
    public List<PessoaHistoricoResponse> listarPorPessoa(UUID pessoaId) {
        garantirPessoaExiste(pessoaId);
        return historicoRepository.findByPessoaIdOrderByDataRegistroDescCreatedAtDesc(pessoaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PessoaHistoricoResponse buscarPorId(UUID pessoaId, UUID historicoId) {
        return toResponse(buscarEntidade(pessoaId, historicoId));
    }

    @Transactional
    public PessoaHistoricoResponse criar(UUID pessoaId, PessoaHistoricoCreateRequest request) {
        garantirPessoaExiste(pessoaId);
        if (request.autorPessoaId() != null) {
            garantirPessoaExiste(request.autorPessoaId());
        }

        PessoaHistorico historico = new PessoaHistorico();
        historico.setPessoaId(pessoaId);
        historico.setDescricao(request.descricao().trim());
        historico.setAutorNome(request.autorNome().trim());
        historico.setAutorCargo(blankToNull(request.autorCargo()));
        historico.setAutorPessoaId(request.autorPessoaId());
        historico.setDataRegistro(request.dataRegistro() != null ? request.dataRegistro() : LocalDate.now());

        return toResponse(historicoRepository.save(historico));
    }

    @Transactional
    public PessoaHistoricoResponse atualizar(
            UUID pessoaId,
            UUID historicoId,
            PessoaHistoricoUpdateRequest request
    ) {
        PessoaHistorico historico = buscarEntidade(pessoaId, historicoId);

        if (request.autorPessoaId() != null) {
            garantirPessoaExiste(request.autorPessoaId());
        }

        historico.setDescricao(request.descricao().trim());
        historico.setAutorNome(request.autorNome().trim());
        historico.setAutorCargo(blankToNull(request.autorCargo()));
        historico.setAutorPessoaId(request.autorPessoaId());
        historico.setDataRegistro(request.dataRegistro());

        return toResponse(historicoRepository.save(historico));
    }

    @Transactional
    public void excluir(UUID pessoaId, UUID historicoId) {
        PessoaHistorico historico = buscarEntidade(pessoaId, historicoId);
        historicoRepository.delete(historico);
    }

    private PessoaHistorico buscarEntidade(UUID pessoaId, UUID historicoId) {
        garantirPessoaExiste(pessoaId);
        PessoaHistorico historico = historicoRepository.findById(historicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de histórico não encontrado"));

        if (!historico.getPessoaId().equals(pessoaId)) {
            throw new ResourceNotFoundException("Registro de histórico não encontrado para esta pessoa");
        }
        return historico;
    }

    private void garantirPessoaExiste(UUID pessoaId) {
        if (!pessoaRepository.existsById(pessoaId)) {
            throw new ResourceNotFoundException("Pessoa não encontrada");
        }
    }

    private PessoaHistoricoResponse toResponse(PessoaHistorico historico) {
        return new PessoaHistoricoResponse(
                historico.getId(),
                historico.getPessoaId(),
                historico.getDescricao(),
                historico.getAutorNome(),
                historico.getAutorCargo(),
                historico.getAutorPessoaId(),
                historico.getDataRegistro(),
                historico.getCreatedAt(),
                historico.getUpdatedAt()
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
