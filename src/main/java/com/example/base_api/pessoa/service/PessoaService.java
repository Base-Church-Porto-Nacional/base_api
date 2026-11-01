package com.example.base_api.pessoa.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.jornada.service.JornadaService;
import com.example.base_api.pessoa.dto.PessoaCreateRequest;
import com.example.base_api.pessoa.dto.PessoaResponse;
import com.example.base_api.pessoa.dto.PessoaUpdateRequest;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final JornadaService jornadaService;
    private final AutorizacaoService autorizacaoService;
    private final UsuarioRepository usuarioRepository;

    public PessoaService(
            PessoaRepository pessoaRepository,
            JornadaService jornadaService,
            AutorizacaoService autorizacaoService,
            UsuarioRepository usuarioRepository
    ) {
        this.pessoaRepository = pessoaRepository;
        this.jornadaService = jornadaService;
        this.autorizacaoService = autorizacaoService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<PessoaResponse> listar(UUID igrejaId) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Pessoa> pessoas = pessoaRepository.findByIgrejaIdInOrderByNomeAsc(ids).stream()
                .filter(autorizacaoService::podeIncluirPessoaNaListagem)
                .toList();
        if (pessoas.isEmpty()) {
            return List.of();
        }
        Map<UUID, Usuario> usuarios = usuarioRepository
                .findByPessoaIdIn(pessoas.stream().map(Pessoa::getId).toList()).stream()
                .collect(Collectors.toMap(Usuario::getPessoaId, u -> u, (a, b) -> a));
        return pessoas.stream()
                .map(p -> toResponse(p, usuarios.get(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PessoaResponse buscarPorId(UUID id) {
        Pessoa pessoa = buscarEntidade(id);
        autorizacaoService.assertPodeVerPessoa(pessoa);
        return toResponse(pessoa);
    }

    @Transactional
    public PessoaResponse criar(PessoaCreateRequest request) {
        if (request.email() != null && !request.email().isBlank()) {
            pessoaRepository.findByEmailIgnoreCase(request.email()).ifPresent(p -> {
                throw new BusinessException("Já existe uma pessoa com este e-mail");
            });
        }

        UUID igrejaId = request.igrejaId() != null
                ? request.igrejaId()
                : autorizacaoService.requireUsuario().getIgrejaId();
        if (igrejaId == null) {
            throw new BusinessException("igrejaId é obrigatório");
        }
        autorizacaoService.assertPodeMutarPessoas(igrejaId);

        Pessoa pessoa = new Pessoa();
        pessoa.setNome(request.nome().trim());
        pessoa.setNomeSocial(blankToNull(request.nomeSocial()));
        pessoa.setEmail(blankToNull(request.email()));
        pessoa.setTelefone(blankToNull(request.telefone()));
        pessoa.setWhatsapp(blankToNull(request.whatsapp()));
        pessoa.setDataNascimento(request.dataNascimento());
        pessoa.setDataCadastro(LocalDate.now());
        pessoa.setIgrejaId(igrejaId);
        pessoa.setAtivo(true);
        pessoa.setObservacoes(request.observacoes());

        Pessoa salva = pessoaRepository.save(pessoa);
        jornadaService.criarJornadaInicial(salva.getId());
        return toResponse(salva);
    }

    @Transactional
    public PessoaResponse atualizar(UUID id, PessoaUpdateRequest request) {
        Pessoa pessoa = buscarEntidade(id);
        autorizacaoService.assertPodeMutarPessoas(pessoa.getIgrejaId());

        if (request.email() != null && !request.email().isBlank()
                && pessoaRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new BusinessException("Já existe uma pessoa com este e-mail");
        }

        pessoa.setNome(request.nome().trim());
        pessoa.setNomeSocial(blankToNull(request.nomeSocial()));
        pessoa.setEmail(blankToNull(request.email()));
        pessoa.setTelefone(blankToNull(request.telefone()));
        pessoa.setWhatsapp(blankToNull(request.whatsapp()));
        pessoa.setDataNascimento(request.dataNascimento());
        pessoa.setAtivo(request.ativo());
        pessoa.setObservacoes(request.observacoes());

        return toResponse(pessoaRepository.save(pessoa));
    }

    @Transactional
    public void excluir(UUID id) {
        Pessoa pessoa = buscarEntidade(id);
        autorizacaoService.assertPodeMutarPessoas(pessoa.getIgrejaId());
        pessoaRepository.delete(pessoa);
    }

    @Transactional(readOnly = true)
    public Pessoa buscarEntidade(UUID id) {
        return pessoaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada"));
    }

    /** Nomes das pessoas por id, numa única consulta (ids nulos são ignorados). */
    @Transactional(readOnly = true)
    public Map<UUID, String> nomesPorId(Collection<UUID> ids) {
        return pessoaRepository.nomesPorId(ids);
    }

    public PessoaResponse toResponse(Pessoa pessoa) {
        return toResponse(pessoa, usuarioRepository.findByPessoaId(pessoa.getId()).orElse(null));
    }

    private PessoaResponse toResponse(Pessoa pessoa, Usuario usuario) {
        Set<String> papeis = usuario == null
                ? Set.of()
                : usuario.nomesPapeis(pessoa.getIgrejaId());
        return new PessoaResponse(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getNomeSocial(),
                pessoa.getEmail(),
                pessoa.getTelefone(),
                pessoa.getWhatsapp(),
                pessoa.getDataNascimento(),
                pessoa.getDataCadastro(),
                pessoa.getAtivo(),
                pessoa.getObservacoes(),
                pessoa.getIgrejaId(),
                usuario != null ? usuario.getId() : null,
                papeis,
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
