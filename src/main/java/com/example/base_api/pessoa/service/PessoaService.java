package com.example.base_api.pessoa.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.jornada.service.JornadaService;
import com.example.base_api.pessoa.dto.PessoaCreateRequest;
import com.example.base_api.pessoa.dto.PessoaResponse;
import com.example.base_api.pessoa.dto.PessoaUpdateRequest;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final JornadaService jornadaService;

    public PessoaService(PessoaRepository pessoaRepository, JornadaService jornadaService) {
        this.pessoaRepository = pessoaRepository;
        this.jornadaService = jornadaService;
    }

    @Transactional(readOnly = true)
    public List<PessoaResponse> listar() {
        return pessoaRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PessoaResponse buscarPorId(UUID id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public PessoaResponse criar(PessoaCreateRequest request) {
        if (request.email() != null && !request.email().isBlank()) {
            pessoaRepository.findByEmailIgnoreCase(request.email()).ifPresent(p -> {
                throw new BusinessException("Já existe uma pessoa com este e-mail");
            });
        }

        Pessoa pessoa = new Pessoa();
        pessoa.setNome(request.nome().trim());
        pessoa.setNomeSocial(blankToNull(request.nomeSocial()));
        pessoa.setEmail(blankToNull(request.email()));
        pessoa.setTelefone(blankToNull(request.telefone()));
        pessoa.setDataNascimento(request.dataNascimento());
        pessoa.setDataCadastro(LocalDate.now());
        pessoa.setAtivo(true);
        pessoa.setObservacoes(request.observacoes());

        Pessoa salva = pessoaRepository.save(pessoa);
        jornadaService.criarJornadaInicial(salva.getId());
        return toResponse(salva);
    }

    @Transactional
    public PessoaResponse atualizar(UUID id, PessoaUpdateRequest request) {
        Pessoa pessoa = buscarEntidade(id);

        if (request.email() != null && !request.email().isBlank()
                && pessoaRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new BusinessException("Já existe uma pessoa com este e-mail");
        }

        pessoa.setNome(request.nome().trim());
        pessoa.setNomeSocial(blankToNull(request.nomeSocial()));
        pessoa.setEmail(blankToNull(request.email()));
        pessoa.setTelefone(blankToNull(request.telefone()));
        pessoa.setDataNascimento(request.dataNascimento());
        pessoa.setAtivo(request.ativo());
        pessoa.setObservacoes(request.observacoes());

        return toResponse(pessoaRepository.save(pessoa));
    }

    @Transactional
    public void excluir(UUID id) {
        Pessoa pessoa = buscarEntidade(id);
        pessoaRepository.delete(pessoa);
    }

    @Transactional(readOnly = true)
    public Pessoa buscarEntidade(UUID id) {
        return pessoaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada"));
    }

    public PessoaResponse toResponse(Pessoa pessoa) {
        return new PessoaResponse(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getNomeSocial(),
                pessoa.getEmail(),
                pessoa.getTelefone(),
                pessoa.getDataNascimento(),
                pessoa.getDataCadastro(),
                pessoa.getAtivo(),
                pessoa.getObservacoes(),
                pessoa.getCreatedAt(),
                pessoa.getUpdatedAt()
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
