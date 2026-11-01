package com.example.base_api.smallgroup.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.smallgroup.dto.SmallGroupCreateRequest;
import com.example.base_api.smallgroup.dto.SmallGroupEncontroRequest;
import com.example.base_api.smallgroup.dto.SmallGroupEncontroResponse;
import com.example.base_api.smallgroup.dto.SmallGroupParticipanteRequest;
import com.example.base_api.smallgroup.dto.SmallGroupParticipanteResponse;
import com.example.base_api.smallgroup.dto.SmallGroupResponse;
import com.example.base_api.smallgroup.dto.SmallGroupUpdateRequest;
import com.example.base_api.smallgroup.entity.SmallGroup;
import com.example.base_api.smallgroup.entity.SmallGroupEncontro;
import com.example.base_api.smallgroup.entity.SmallGroupParticipante;
import com.example.base_api.smallgroup.repository.SmallGroupEncontroRepository;
import com.example.base_api.smallgroup.repository.SmallGroupParticipanteRepository;
import com.example.base_api.smallgroup.repository.SmallGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SmallGroupService {

    private final SmallGroupRepository smallGroupRepository;
    private final SmallGroupEncontroRepository encontroRepository;
    private final SmallGroupParticipanteRepository participanteRepository;
    private final MinisterioRepository ministerioRepository;
    private final PessoaRepository pessoaRepository;
    private final AutorizacaoService autorizacaoService;

    public SmallGroupService(
            SmallGroupRepository smallGroupRepository,
            SmallGroupEncontroRepository encontroRepository,
            SmallGroupParticipanteRepository participanteRepository,
            MinisterioRepository ministerioRepository,
            PessoaRepository pessoaRepository,
            AutorizacaoService autorizacaoService
    ) {
        this.smallGroupRepository = smallGroupRepository;
        this.encontroRepository = encontroRepository;
        this.participanteRepository = participanteRepository;
        this.ministerioRepository = ministerioRepository;
        this.pessoaRepository = pessoaRepository;
        this.autorizacaoService = autorizacaoService;
    }

    @Transactional(readOnly = true)
    public List<SmallGroupResponse> listar(UUID igrejaId) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<SmallGroup> grupos = smallGroupRepository.findByIgrejaIdInOrderByNomeAsc(ids);
        return toResponse(grupos);
    }

    @Transactional(readOnly = true)
    public SmallGroupResponse buscarPorId(UUID id) {
        SmallGroup grupo = buscarEntidade(id);
        autorizacaoService.assertAcesso(grupo.getIgrejaId());
        return toResponse(List.of(grupo)).get(0);
    }

    @Transactional
    public SmallGroupResponse criar(SmallGroupCreateRequest request) {
        autorizacaoService.assertPodeCriarSmallGroup(request.igrejaId());
        smallGroupRepository.findByIgrejaIdAndNomeIgnoreCase(request.igrejaId(), request.nome().trim())
                .ifPresent(g -> {
                    throw new BusinessException("Já existe um pequeno grupo com este nome nesta igreja");
                });

        SmallGroup grupo = new SmallGroup();
        grupo.setNome(request.nome().trim());
        grupo.setIgrejaId(request.igrejaId());
        grupo.setDiaSemana(request.diaSemana());
        grupo.setHorario(request.horario());
        grupo.setLiderPessoaId(request.liderPessoaId());
        grupo.setMinisterioId(validarMinisterio(request.igrejaId(), request.ministerioId()));
        grupo.setAtivo(request.ativo() == null || request.ativo());
        grupo.setNota(request.nota() == null ? 0 : request.nota());

        return toResponse(List.of(smallGroupRepository.save(grupo))).get(0);
    }

    @Transactional
    public SmallGroupResponse atualizar(UUID id, SmallGroupUpdateRequest request) {
        SmallGroup grupo = buscarEntidade(id);
        autorizacaoService.assertPodeGerenciarSmallGroup(grupo);

        if (smallGroupRepository.existsByIgrejaIdAndNomeIgnoreCaseAndIdNot(
                grupo.getIgrejaId(), request.nome().trim(), id)) {
            throw new BusinessException("Já existe um pequeno grupo com este nome nesta igreja");
        }

        grupo.setNome(request.nome().trim());
        grupo.setDiaSemana(request.diaSemana());
        grupo.setHorario(request.horario());
        grupo.setLiderPessoaId(request.liderPessoaId());
        grupo.setMinisterioId(validarMinisterio(grupo.getIgrejaId(), request.ministerioId()));
        grupo.setAtivo(request.ativo());
        grupo.setNota(request.nota() == null ? 0 : request.nota());

        return toResponse(List.of(smallGroupRepository.save(grupo))).get(0);
    }

    @Transactional
    public void excluir(UUID id) {
        SmallGroup grupo = buscarEntidade(id);
        autorizacaoService.assertPodeCriarSmallGroup(grupo.getIgrejaId());
        smallGroupRepository.delete(grupo);
    }

    @Transactional
    public SmallGroupEncontroResponse criarEncontro(UUID smallGroupId, SmallGroupEncontroRequest request) {
        SmallGroup grupo = buscarEntidade(smallGroupId);
        autorizacaoService.assertPodeGerenciarSmallGroup(grupo);

        encontroRepository.findFirstBySmallGroupIdAndData(smallGroupId, request.data()).ifPresent(e -> {
            throw new BusinessException("Já existe um encontro deste grupo nesta data");
        });

        SmallGroupEncontro encontro = new SmallGroupEncontro();
        encontro.setSmallGroupId(smallGroupId);
        aplicar(request, encontro);
        SmallGroupEncontro salvo = encontroRepository.save(encontro);
        salvarParticipantes(salvo.getId(), request.compareceram());
        return toEncontroResponse(salvo);
    }

    @Transactional
    public SmallGroupEncontroResponse atualizarEncontro(UUID encontroId, SmallGroupEncontroRequest request) {
        SmallGroupEncontro encontro = encontroRepository.findById(encontroId)
                .orElseThrow(() -> new ResourceNotFoundException("Encontro não encontrado"));
        SmallGroup grupo = buscarEntidade(encontro.getSmallGroupId());
        autorizacaoService.assertPodeGerenciarSmallGroup(grupo);

        if (request.data() != null && !request.data().equals(encontro.getData())) {
            encontroRepository.findFirstBySmallGroupIdAndData(grupo.getId(), request.data()).ifPresent(e -> {
                throw new BusinessException("Já existe um encontro deste grupo nesta data");
            });
        }

        aplicar(request, encontro);
        SmallGroupEncontro salvo = encontroRepository.save(encontro);

        participanteRepository.deleteByEncontroId(salvo.getId());
        participanteRepository.flush();
        salvarParticipantes(salvo.getId(), request.compareceram());
        return toEncontroResponse(salvo);
    }

    private void aplicar(SmallGroupEncontroRequest request, SmallGroupEncontro encontro) {
        encontro.setData(request.data());
        encontro.setAnfitriaoPessoaId(request.anfitriaoPessoaId());
        encontro.setAnotacoes(request.anotacoes());
    }

    private void salvarParticipantes(UUID encontroId, List<SmallGroupParticipanteRequest> compareceram) {
        if (compareceram == null || compareceram.isEmpty()) {
            return;
        }
        Map<UUID, String> nomes = pessoaRepository.nomesPorId(
                compareceram.stream().map(SmallGroupParticipanteRequest::pessoaId).toList());
        participanteRepository.saveAll(compareceram.stream().map(p -> {
            SmallGroupParticipante participante = new SmallGroupParticipante();
            participante.setEncontroId(encontroId);
            participante.setPessoaId(p.pessoaId());
            participante.setPessoaNome(Objects.requireNonNullElse(porId(nomes, p.pessoaId()), p.pessoaNome()));
            return participante;
        }).toList());
    }

    @Transactional(readOnly = true)
    public SmallGroup buscarEntidade(UUID id) {
        return smallGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pequeno grupo não encontrado"));
    }

    private List<SmallGroupResponse> toResponse(List<SmallGroup> grupos) {
        if (grupos.isEmpty()) {
            return List.of();
        }
        List<UUID> grupoIds = grupos.stream().map(SmallGroup::getId).toList();
        List<SmallGroupEncontro> encontros = encontroRepository.findBySmallGroupIdInOrderByDataAsc(grupoIds);
        Map<UUID, List<SmallGroupEncontro>> encontrosPorGrupo = encontros.stream()
                .collect(Collectors.groupingBy(SmallGroupEncontro::getSmallGroupId));
        Map<UUID, List<SmallGroupParticipante>> participantes = participanteRepository
                .findByEncontroIdInOrderByPessoaNomeAsc(encontros.stream().map(SmallGroupEncontro::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(SmallGroupParticipante::getEncontroId));

        Map<UUID, Ministerio> ministerios = ministerioRepository
                .findAllById(grupos.stream().map(SmallGroup::getMinisterioId).filter(Objects::nonNull).toList())
                .stream()
                .collect(Collectors.toMap(Ministerio::getId, m -> m));

        Map<UUID, String> nomes = pessoaRepository.nomesPorId(Stream.concat(
                grupos.stream().map(SmallGroup::getLiderPessoaId),
                encontros.stream().map(SmallGroupEncontro::getAnfitriaoPessoaId)).toList());

        return grupos.stream().map(g -> toResponse(g, encontrosPorGrupo, participantes, ministerios, nomes)).toList();
    }

    private SmallGroupResponse toResponse(
            SmallGroup grupo,
            Map<UUID, List<SmallGroupEncontro>> encontrosPorGrupo,
            Map<UUID, List<SmallGroupParticipante>> participantesPorEncontro,
            Map<UUID, Ministerio> ministerios,
            Map<UUID, String> nomes
    ) {
        List<SmallGroupEncontroResponse> encontros = encontrosPorGrupo
                .getOrDefault(grupo.getId(), List.of()).stream()
                .map(e -> toEncontroResponse(e,
                        participantesPorEncontro.getOrDefault(e.getId(), List.<SmallGroupParticipante>of()).stream()
                                .map(this::toParticipanteResponse)
                                .toList(), nomes))
                .toList();

        Ministerio ministerio = grupo.getMinisterioId() == null ? null : ministerios.get(grupo.getMinisterioId());
        return new SmallGroupResponse(
                grupo.getId(),
                grupo.getNome(),
                grupo.getIgrejaId(),
                grupo.getDiaSemana(),
                grupo.getHorario(),
                grupo.getLiderPessoaId(),
                porId(nomes, grupo.getLiderPessoaId()),
                grupo.getMinisterioId(),
                ministerio == null ? null : ministerio.getNome(),
                grupo.getAtivo(),
                grupo.getNota(),
                encontros,
                grupo.getCreatedAt(),
                grupo.getUpdatedAt()
        );
    }

    private SmallGroupEncontroResponse toEncontroResponse(SmallGroupEncontro encontro) {
        List<SmallGroupParticipanteResponse> compareceram = participanteRepository
                .findByEncontroIdOrderByPessoaNomeAsc(encontro.getId()).stream()
                .map(this::toParticipanteResponse)
                .toList();
        return toEncontroResponse(encontro, compareceram,
                pessoaRepository.nomesPorId(Collections.singletonList(encontro.getAnfitriaoPessoaId())));
    }

    private SmallGroupEncontroResponse toEncontroResponse(
            SmallGroupEncontro encontro,
            List<SmallGroupParticipanteResponse> compareceram,
            Map<UUID, String> nomes
    ) {
        return new SmallGroupEncontroResponse(
                encontro.getId(),
                encontro.getSmallGroupId(),
                encontro.getData(),
                encontro.getAnfitriaoPessoaId(),
                porId(nomes, encontro.getAnfitriaoPessoaId()),
                compareceram,
                encontro.getAnotacoes(),
                encontro.getCreatedAt(),
                encontro.getUpdatedAt()
        );
    }

    private SmallGroupParticipanteResponse toParticipanteResponse(SmallGroupParticipante participante) {
        return new SmallGroupParticipanteResponse(
                participante.getPessoaId(),
                participante.getPessoaNome()
        );
    }

    private UUID validarMinisterio(UUID igrejaId, UUID ministerioId) {
        if (ministerioId == null) {
            return null;
        }
        Ministerio ministerio = ministerioRepository.findById(ministerioId)
                .orElseThrow(() -> new BusinessException("Ministério não encontrado"));
        if (!igrejaId.equals(ministerio.getIgrejaId())) {
            throw new BusinessException("O ministério não pertence a esta igreja");
        }
        return ministerio.getId();
    }

    private static String porId(Map<UUID, String> nomes, UUID id) {
        return id == null ? null : nomes.get(id);
    }
}