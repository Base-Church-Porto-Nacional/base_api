package com.example.base_api.ministerio.service;

import com.example.base_api.baseministerial.entity.BaseMinisterial;
import com.example.base_api.baseministerial.repository.BaseMinisterialRepository;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.ministerio.dto.MinisterioCreateRequest;
import com.example.base_api.ministerio.dto.MinisterioResponse;
import com.example.base_api.ministerio.dto.MinisterioUpdateRequest;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.entity.MinisterioTipo;
import com.example.base_api.ministerio.repository.MinisterioRepository;
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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MinisterioService {

    private final MinisterioRepository ministerioRepository;
    private final AutorizacaoService autorizacaoService;
    private final PessoaRepository pessoaRepository;
    private final ServicoRepository servicoRepository;
    private final PessoaServicoRepository pessoaServicoRepository;
    private final BaseMinisterialRepository baseMinisterialRepository;

    public MinisterioService(
            MinisterioRepository ministerioRepository,
            AutorizacaoService autorizacaoService,
            PessoaRepository pessoaRepository,
            ServicoRepository servicoRepository,
            PessoaServicoRepository pessoaServicoRepository,
            BaseMinisterialRepository baseMinisterialRepository
    ) {
        this.ministerioRepository = ministerioRepository;
        this.autorizacaoService = autorizacaoService;
        this.pessoaRepository = pessoaRepository;
        this.servicoRepository = servicoRepository;
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.baseMinisterialRepository = baseMinisterialRepository;
    }

    @Transactional(readOnly = true)
    public List<MinisterioResponse> listar(UUID igrejaId) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return List.of();
        }
        return toResponse(ministerioRepository.findByIgrejaIdInOrderByNomeAsc(ids));
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
        ministerioRepository.findByIgrejaIdAndNomeIgnoreCase(request.igrejaId(), request.nome().trim()).ifPresent(m -> {
            throw new BusinessException("Já existe um ministério com este nome nesta igreja");
        });

        Ministerio ministerio = new Ministerio();
        ministerio.setNome(request.nome().trim());
        ministerio.setDescricao(request.descricao());
        ministerio.setIgrejaId(request.igrejaId());
        ministerio.setTipo(request.tipo() == null ? MinisterioTipo.GERAL : request.tipo());
        ministerio.setLiderPessoaId(request.liderPessoaId());
        ministerio.setCoordenadorPessoaId(request.coordenadorPessoaId());
        ministerio.setBaseMinisterialId(validarBase(request.igrejaId(), request.baseMinisterialId()));
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
                ministerio.getIgrejaId(), request.nome().trim(), id)) {
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
        ministerio.setBaseMinisterialId(validarBase(ministerio.getIgrejaId(), request.baseMinisterialId()));
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

        List<UUID> servicoIds = servicoRepository.findByMinisterioIdOrderByNomeAsc(id)
                .stream()
                .map(Servico::getId)
                .toList();
        if (!servicoIds.isEmpty()) {
            pessoaServicoRepository.deleteByServicoIdIn(servicoIds);
            servicoRepository.deleteByMinisterioId(id);
        }
        ministerioRepository.delete(ministerio);
    }

    @Transactional(readOnly = true)
    public Ministerio buscarEntidade(UUID id) {
        return ministerioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ministério não encontrado"));
    }

    public MinisterioResponse toResponse(Ministerio ministerio) {
        return toResponse(List.of(ministerio)).get(0);
    }

    /** Monta as respostas carregando serviços, voluntários, pessoas e bases em lote. */
    private List<MinisterioResponse> toResponse(List<Ministerio> ministerios) {
        if (ministerios.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<Servico>> servicosPorMinisterio = servicoRepository
                .findByMinisterioIdInOrderByNomeAsc(ministerios.stream().map(Ministerio::getId).toList()).stream()
                .collect(Collectors.groupingBy(Servico::getMinisterioId));

        Map<UUID, UUID> ministerioDoServicoReal = servicosPorMinisterio.values().stream()
                .flatMap(List::stream)
                .filter(s -> !Boolean.TRUE.equals(s.getFiccao()))
                .collect(Collectors.toMap(Servico::getId, Servico::getMinisterioId));
        Map<UUID, Long> voluntariosPorMinisterio = ministerioDoServicoReal.isEmpty()
                ? Map.of()
                : pessoaServicoRepository.findByServicoIdInAndAtivoTrue(List.copyOf(ministerioDoServicoReal.keySet()))
                .stream()
                .filter(v -> ministerioDoServicoReal.containsKey(v.getServicoId()))
                .collect(Collectors.groupingBy(
                        v -> ministerioDoServicoReal.get(v.getServicoId()),
                        Collectors.mapping(PessoaServico::getPessoaId,
                                Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size()))));

        Map<UUID, String> nomesPessoas = pessoaRepository.nomesPorId(ministerios.stream()
                .flatMap(m -> Stream.of(m.getLiderPessoaId(), m.getCoordenadorPessoaId()))
                .toList());
        List<UUID> baseIds = ministerios.stream()
                .map(Ministerio::getBaseMinisterialId).filter(Objects::nonNull).distinct().toList();
        Map<UUID, String> nomesBases = baseIds.isEmpty()
                ? Map.of()
                : baseMinisterialRepository.findAllById(baseIds).stream()
                .collect(Collectors.toMap(BaseMinisterial::getId, BaseMinisterial::getNome));

        return ministerios.stream().map(ministerio -> {
            List<Servico> servicos = servicosPorMinisterio.getOrDefault(ministerio.getId(), List.of());
            return new MinisterioResponse(
                    ministerio.getId(),
                    ministerio.getNome(),
                    ministerio.getDescricao(),
                    ministerio.getAtivo(),
                    ministerio.getIgrejaId(),
                    ministerio.getTipo(),
                    ministerio.getLiderPessoaId(),
                    porId(nomesPessoas, ministerio.getLiderPessoaId()),
                    ministerio.getCoordenadorPessoaId(),
                    porId(nomesPessoas, ministerio.getCoordenadorPessoaId()),
                    ministerio.getBaseMinisterialId(),
                    porId(nomesBases, ministerio.getBaseMinisterialId()),
                    ministerio.getAnotacoes(),
                    ministerio.getOrdem(),
                    voluntariosPorMinisterio.getOrDefault(ministerio.getId(), 0L).intValue(),
                    servicos.size(),
                    servicos.stream().map(this::toServicoResponse).toList(),
                    ministerio.getCreatedAt(),
                    ministerio.getUpdatedAt()
            );
        }).toList();
    }

    private static String porId(Map<UUID, String> nomes, UUID id) {
        return id == null ? null : nomes.get(id);
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
        Servico coordenador = servicoRepository.findByMinisterioIdAndNomeIgnoreCase(
                ministerio.getId(), Ministerio.SERVICO_COORDENADOR
        ).orElse(null);
        if (coordenador == null) {
            return;
        }
        pessoaServicoRepository.findByServicoIdAndAtivoTrue(coordenador.getId())
                .forEach(v -> {
                    v.setAtivo(false);
                    pessoaServicoRepository.save(v);
                });
        if (ministerio.getCoordenadorPessoaId() == null) {
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


    private UUID validarBase(UUID igrejaId, UUID baseMinisterialId) {
        if (baseMinisterialId == null) {
            return null;
        }
        BaseMinisterial base = baseMinisterialRepository.findById(baseMinisterialId)
                .orElseThrow(() -> new BusinessException("Base ministerial não encontrada"));
        if (!igrejaId.equals(base.getIgrejaId())) {
            throw new BusinessException("A base ministerial não pertence a esta igreja");
        }
        return base.getId();
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
