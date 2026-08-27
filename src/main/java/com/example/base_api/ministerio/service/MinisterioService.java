package com.example.base_api.ministerio.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.ministerio.dto.MinisterioCreateRequest;
import com.example.base_api.ministerio.dto.MinisterioResponse;
import com.example.base_api.ministerio.dto.MinisterioUpdateRequest;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.entity.MinisterioTipo;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.dto.ServicoResponse;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MinisterioService {

    private final MinisterioRepository ministerioRepository;
    private final AutorizacaoService autorizacaoService;
    private final PessoaRepository pessoaRepository;
    private final ServicoRepository servicoRepository;
    private final PessoaServicoRepository pessoaServicoRepository;

    public MinisterioService(
            MinisterioRepository ministerioRepository,
            AutorizacaoService autorizacaoService,
            PessoaRepository pessoaRepository,
            ServicoRepository servicoRepository,
            PessoaServicoRepository pessoaServicoRepository
    ) {
        this.ministerioRepository = ministerioRepository;
        this.autorizacaoService = autorizacaoService;
        this.pessoaRepository = pessoaRepository;
        this.servicoRepository = servicoRepository;
        this.pessoaServicoRepository = pessoaServicoRepository;
    }

    @Transactional(readOnly = true)
    public List<MinisterioResponse> listar(UUID igrejaId) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return List.of();
        }
        return ministerioRepository.findByIgrejaIdInOrderByNomeAsc(ids).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MinisterioResponse buscarPorId(UUID id) {
        Ministerio ministerio = buscarEntidade(id);
        autorizacaoService.assertAcesso(ministerio.getIgrejaId());
        return toResponse(ministerio);
    }

    @Transactional
    public MinisterioResponse criar(MinisterioCreateRequest request) {
        autorizacaoService.assertPodeCriarMinisterio(request.igrejaId());
        ministerioRepository.findByIgrejaIdAndNomeIgnoreCase(request.igrejaId(), request.nome()).ifPresent(m -> {
            throw new BusinessException("Já existe um ministério com este nome nesta igreja");
        });

        Ministerio ministerio = new Ministerio();
        ministerio.setNome(request.nome().trim());
        ministerio.setDescricao(request.descricao());
        ministerio.setIgrejaId(request.igrejaId());
        ministerio.setTipo(request.tipo() == null ? MinisterioTipo.GERAL : request.tipo());
        ministerio.setLiderPessoaId(request.liderPessoaId());
        ministerio.setCoordenadorPessoaId(request.coordenadorPessoaId());
        ministerio.setAtivo(request.ativo() == null || request.ativo());

        Ministerio salvo = ministerioRepository.save(ministerio);
        garantirServicoCoordenador(salvo);
        sincronizarCoordenador(salvo);
        return toResponse(salvo);
    }

    @Transactional
    public MinisterioResponse atualizar(UUID id, MinisterioUpdateRequest request) {
        Ministerio ministerio = buscarEntidade(id);
        autorizacaoService.assertPodeGerenciarMinisterio(ministerio);

        if (ministerioRepository.existsByIgrejaIdAndNomeIgnoreCaseAndIdNot(
                ministerio.getIgrejaId(), request.nome(), id)) {
            throw new BusinessException("Já existe um ministério com este nome nesta igreja");
        }

        ministerio.setNome(request.nome().trim());
        ministerio.setDescricao(request.descricao());
        ministerio.setAtivo(request.ativo());
        if (request.tipo() != null) {
            ministerio.setTipo(request.tipo());
        }
        ministerio.setLiderPessoaId(request.liderPessoaId());
        ministerio.setCoordenadorPessoaId(request.coordenadorPessoaId());
        ministerio.setAnotacoes(request.anotacoes());
        ministerio.setOrdem(request.ordem());

        Ministerio salvo = ministerioRepository.save(ministerio);
        garantirServicoCoordenador(salvo);
        sincronizarCoordenador(salvo);
        return toResponse(salvo);
    }

    @Transactional
    public void excluir(UUID id) {
        Ministerio ministerio = buscarEntidade(id);
        autorizacaoService.assertPodeCriarMinisterio(ministerio.getIgrejaId());
        ministerioRepository.delete(ministerio);
    }

    @Transactional(readOnly = true)
    public Ministerio buscarEntidade(UUID id) {
        return ministerioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ministério não encontrado"));
    }

    public MinisterioResponse toResponse(Ministerio ministerio) {
        List<Servico> servicos = servicoRepository.findByMinisterioIdOrderByNomeAsc(ministerio.getId());
        List<UUID> servicoIds = servicos.stream().map(Servico::getId).toList();
        long voluntarios = servicoIds.isEmpty()
                ? 0
                : pessoaServicoRepository.findByServicoIdInAndAtivoTrue(servicoIds).stream()
                .filter(v -> {
                    Servico s = servicos.stream()
                            .filter(item -> item.getId().equals(v.getServicoId()))
                            .findFirst()
                            .orElse(null);
                    return s != null && !Boolean.TRUE.equals(s.getFiccao());
                })
                .map(PessoaServico::getPessoaId)
                .distinct()
                .count();

        return new MinisterioResponse(
                ministerio.getId(),
                ministerio.getNome(),
                ministerio.getDescricao(),
                ministerio.getAtivo(),
                ministerio.getIgrejaId(),
                ministerio.getTipo(),
                ministerio.getLiderPessoaId(),
                nomePessoa(ministerio.getLiderPessoaId()),
                ministerio.getCoordenadorPessoaId(),
                nomePessoa(ministerio.getCoordenadorPessoaId()),
                ministerio.getAnotacoes(),
                ministerio.getOrdem(),
                (int) voluntarios,
                servicos.size(),
                servicos.stream().map(this::toServicoResponse).toList(),
                ministerio.getCreatedAt(),
                ministerio.getUpdatedAt()
        );
    }

    private void garantirServicoCoordenador(Ministerio ministerio) {
        servicoRepository.findByMinisterioIdAndNomeIgnoreCase(
                ministerio.getId(), Ministerio.SERVICO_COORDENADOR
        ).orElseGet(() -> {
            Servico coordenador = new Servico();
            coordenador.setMinisterioId(ministerio.getId());
            coordenador.setNome(Ministerio.SERVICO_COORDENADOR);
            coordenador.setDescricao("Serviço ficção — não entra na escala de voluntários");
            coordenador.setFiccao(true);
            coordenador.setAtivo(true);
            return servicoRepository.save(coordenador);
        });
    }

    private void sincronizarCoordenador(Ministerio ministerio) {
        if (ministerio.getCoordenadorPessoaId() == null) {
            return;
        }
        Servico coordenador = servicoRepository.findByMinisterioIdAndNomeIgnoreCase(
                ministerio.getId(), Ministerio.SERVICO_COORDENADOR
        ).orElse(null);
        if (coordenador == null) {
            return;
        }
        pessoaServicoRepository.findByPessoaIdAndServicoId(
                ministerio.getCoordenadorPessoaId(), coordenador.getId()
        ).ifPresentOrElse(vinculo -> {
            vinculo.setAtivo(true);
            pessoaServicoRepository.save(vinculo);
        }, () -> {
            PessoaServico vinculo = new PessoaServico();
            vinculo.setPessoaId(ministerio.getCoordenadorPessoaId());
            vinculo.setServicoId(coordenador.getId());
            vinculo.setAtivo(true);
            pessoaServicoRepository.save(vinculo);
        });
    }

    private String nomePessoa(UUID pessoaId) {
        if (pessoaId == null) {
            return null;
        }
        return pessoaRepository.findById(pessoaId).map(Pessoa::getNome).orElse(null);
    }

    private ServicoResponse toServicoResponse(Servico servico) {
        return new ServicoResponse(
                servico.getId(),
                servico.getMinisterioId(),
                servico.getNome(),
                servico.getDescricao(),
                servico.getFiccao(),
                servico.getAtivo(),
                servico.getCreatedAt(),
                servico.getUpdatedAt()
        );
    }
}
