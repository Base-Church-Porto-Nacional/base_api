package com.example.base_api.voluntario.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.service.MinisterioService;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.service.ServicoService;
import com.example.base_api.voluntario.dto.PessoaServicoCreateRequest;
import com.example.base_api.voluntario.dto.PessoaServicoResponse;
import com.example.base_api.voluntario.dto.PessoaServicoUpdateRequest;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class VoluntarioService {

    private final PessoaServicoRepository pessoaServicoRepository;
    private final PessoaService pessoaService;
    private final ServicoService servicoService;
    private final MinisterioService ministerioService;

    public VoluntarioService(
            PessoaServicoRepository pessoaServicoRepository,
            PessoaService pessoaService,
            ServicoService servicoService,
            MinisterioService ministerioService
    ) {
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.pessoaService = pessoaService;
        this.servicoService = servicoService;
        this.ministerioService = ministerioService;
    }

    @Transactional(readOnly = true)
    public List<PessoaServicoResponse> listar() {
        return pessoaServicoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PessoaServicoResponse buscarPorId(UUID id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<PessoaServicoResponse> listarPorPessoa(UUID pessoaId) {
        pessoaService.buscarEntidade(pessoaId);
        return pessoaServicoRepository.findByPessoaId(pessoaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PessoaServicoResponse> listarQualificadosPorServico(UUID servicoId) {
        servicoService.buscarEntidade(servicoId);
        return pessoaServicoRepository.findByServicoIdAndAtivoTrue(servicoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PessoaServicoResponse vincular(PessoaServicoCreateRequest request) {
        Pessoa pessoa = pessoaService.buscarEntidade(request.pessoaId());
        Servico servico = servicoService.buscarEntidade(request.servicoId());

        if (pessoaServicoRepository.existsByPessoaIdAndServicoId(pessoa.getId(), servico.getId())) {
            throw new BusinessException("A pessoa já está vinculada a este serviço");
        }

        if (request.liderResponsavelId() != null) {
            pessoaService.buscarEntidade(request.liderResponsavelId());
        }

        PessoaServico vinculo = new PessoaServico();
        vinculo.setPessoaId(pessoa.getId());
        vinculo.setServicoId(servico.getId());
        vinculo.setCargo(blankToNull(request.cargo()));
        vinculo.setDataEntrada(request.dataEntrada() != null ? request.dataEntrada() : LocalDate.now());
        vinculo.setAtivo(true);
        vinculo.setObservacoes(request.observacoes());
        vinculo.setLiderResponsavelId(request.liderResponsavelId());

        return toResponse(pessoaServicoRepository.save(vinculo));
    }

    @Transactional
    public PessoaServicoResponse atualizar(UUID id, PessoaServicoUpdateRequest request) {
        PessoaServico vinculo = buscarEntidade(id);
        Pessoa pessoa = pessoaService.buscarEntidade(request.pessoaId());
        Servico servico = servicoService.buscarEntidade(request.servicoId());

        if (!vinculo.getPessoaId().equals(pessoa.getId()) || !vinculo.getServicoId().equals(servico.getId())) {
            pessoaServicoRepository.findByPessoaIdAndServicoId(pessoa.getId(), servico.getId())
                    .filter(outro -> !outro.getId().equals(id))
                    .ifPresent(outro -> {
                        throw new BusinessException("A pessoa já está vinculada a este serviço");
                    });
        }

        if (request.liderResponsavelId() != null) {
            pessoaService.buscarEntidade(request.liderResponsavelId());
        }

        vinculo.setPessoaId(pessoa.getId());
        vinculo.setServicoId(servico.getId());
        vinculo.setCargo(blankToNull(request.cargo()));
        vinculo.setDataEntrada(request.dataEntrada());
        vinculo.setAtivo(request.ativo());
        vinculo.setObservacoes(request.observacoes());
        vinculo.setLiderResponsavelId(request.liderResponsavelId());

        return toResponse(pessoaServicoRepository.save(vinculo));
    }

    @Transactional
    public void desvincular(UUID id) {
        PessoaServico vinculo = buscarEntidade(id);
        pessoaServicoRepository.delete(vinculo);
    }

    @Transactional(readOnly = true)
    public PessoaServico buscarEntidade(UUID id) {
        return pessoaServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo de voluntário não encontrado"));
    }

    private PessoaServicoResponse toResponse(PessoaServico vinculo) {
        Pessoa pessoa = pessoaService.buscarEntidade(vinculo.getPessoaId());
        Servico servico = servicoService.buscarEntidade(vinculo.getServicoId());
        Ministerio ministerio = ministerioService.buscarEntidade(servico.getMinisterioId());

        return new PessoaServicoResponse(
                vinculo.getId(),
                vinculo.getPessoaId(),
                pessoa.getNome(),
                vinculo.getServicoId(),
                servico.getNome(),
                ministerio.getId(),
                ministerio.getNome(),
                vinculo.getCargo(),
                vinculo.getDataEntrada(),
                vinculo.getAtivo(),
                vinculo.getObservacoes(),
                vinculo.getLiderResponsavelId(),
                vinculo.getCreatedAt(),
                vinculo.getUpdatedAt()
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
