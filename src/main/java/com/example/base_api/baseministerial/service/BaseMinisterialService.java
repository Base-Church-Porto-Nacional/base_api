package com.example.base_api.baseministerial.service;

import com.example.base_api.baseministerial.dto.BaseMinisterialCreateRequest;
import com.example.base_api.baseministerial.dto.BaseMinisterialResponse;
import com.example.base_api.baseministerial.dto.BaseMinisterialUpdateRequest;
import com.example.base_api.baseministerial.entity.BaseMinisterial;
import com.example.base_api.baseministerial.repository.BaseMinisterialRepository;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class BaseMinisterialService {

    private final BaseMinisterialRepository baseMinisterialRepository;
    private final MinisterioRepository ministerioRepository;
    private final PessoaRepository pessoaRepository;
    private final AutorizacaoService autorizacaoService;

    public BaseMinisterialService(
            BaseMinisterialRepository baseMinisterialRepository,
            MinisterioRepository ministerioRepository,
            PessoaRepository pessoaRepository,
            AutorizacaoService autorizacaoService
    ) {
        this.baseMinisterialRepository = baseMinisterialRepository;
        this.ministerioRepository = ministerioRepository;
        this.pessoaRepository = pessoaRepository;
        this.autorizacaoService = autorizacaoService;
    }

    @Transactional(readOnly = true)
    public List<BaseMinisterialResponse> listar(UUID igrejaId) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<BaseMinisterial> bases = baseMinisterialRepository.findByIgrejaIdInOrderByNomeAsc(ids);
        Map<UUID, String> nomes = pessoaRepository.nomesPorId(bases.stream()
                .flatMap(b -> Stream.of(b.getLiderPessoaId(), b.getCoordenadorPessoaId()))
                .toList());
        return bases.stream().map(b -> toResponse(b, nomes)).toList();
    }

    @Transactional(readOnly = true)
    public BaseMinisterialResponse buscarPorId(UUID id) {
        BaseMinisterial base = buscarEntidade(id);
        autorizacaoService.assertAcesso(base.getIgrejaId());
        return toResponse(base);
    }

    @Transactional
    public BaseMinisterialResponse criar(BaseMinisterialCreateRequest request) {
        autorizacaoService.assertPodeCriarBaseMinisterial(request.igrejaId());
        baseMinisterialRepository.findByIgrejaIdAndNomeIgnoreCase(request.igrejaId(), request.nome().trim())
                .ifPresent(b -> {
                    throw new BusinessException("Já existe uma base ministerial com este nome nesta igreja");
                });

        BaseMinisterial base = new BaseMinisterial();
        base.setNome(request.nome().trim());
        base.setDescricao(request.descricao());
        base.setIgrejaId(request.igrejaId());
        base.setLiderPessoaId(request.liderPessoaId());
        base.setCoordenadorPessoaId(request.coordenadorPessoaId());
        base.setAtivo(request.ativo() == null || request.ativo());

        return toResponse(baseMinisterialRepository.save(base));
    }

    @Transactional
    public BaseMinisterialResponse atualizar(UUID id, BaseMinisterialUpdateRequest request) {
        BaseMinisterial base = buscarEntidade(id);
        autorizacaoService.assertPodeGerenciarBaseMinisterial(base);

        if (baseMinisterialRepository.existsByIgrejaIdAndNomeIgnoreCaseAndIdNot(
                base.getIgrejaId(), request.nome().trim(), id)) {
            throw new BusinessException("Já existe uma base ministerial com este nome nesta igreja");
        }

        base.setNome(request.nome().trim());
        base.setDescricao(request.descricao());
        base.setAtivo(request.ativo());
        base.setLiderPessoaId(request.liderPessoaId());
        base.setCoordenadorPessoaId(request.coordenadorPessoaId());
        base.setOrdem(request.ordem());

        return toResponse(baseMinisterialRepository.save(base));
    }

    @Transactional
    public void excluir(UUID id) {
        BaseMinisterial base = buscarEntidade(id);
        autorizacaoService.assertPodeCriarBaseMinisterial(base.getIgrejaId());

        for (Ministerio ministerio : ministerioRepository.findByBaseMinisterialId(id)) {
            ministerio.setBaseMinisterialId(null);
            ministerioRepository.save(ministerio);
        }
        baseMinisterialRepository.delete(base);
    }

    @Transactional(readOnly = true)
    public BaseMinisterial buscarEntidade(UUID id) {
        return baseMinisterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Base ministerial não encontrada"));
    }

    public BaseMinisterialResponse toResponse(BaseMinisterial base) {
        return toResponse(base, pessoaRepository.nomesPorId(
                Arrays.asList(base.getLiderPessoaId(), base.getCoordenadorPessoaId())));
    }

    private BaseMinisterialResponse toResponse(BaseMinisterial base, Map<UUID, String> nomes) {
        return new BaseMinisterialResponse(
                base.getId(),
                base.getNome(),
                base.getDescricao(),
                base.getAtivo(),
                base.getIgrejaId(),
                base.getLiderPessoaId(),
                porId(nomes, base.getLiderPessoaId()),
                base.getCoordenadorPessoaId(),
                porId(nomes, base.getCoordenadorPessoaId()),
                base.getOrdem(),
                base.getCreatedAt(),
                base.getUpdatedAt()
        );
    }

    private static String porId(Map<UUID, String> nomes, UUID id) {
        return id == null ? null : nomes.get(id);
    }
}
