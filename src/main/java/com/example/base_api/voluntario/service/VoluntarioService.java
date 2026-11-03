package com.example.base_api.voluntario.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.service.MinisterioService;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.AutorizacaoService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.example.base_api.common.util.Textos.blankToNull;

@Service
public class VoluntarioService {

    static final String MSG_SERVICO_FICTICIO =
            "O serviço Coordenador é definido pela coordenação do ministério, não por vínculo de voluntário.";
    static final String MSG_SERVICO_INATIVO = "Este serviço está inativo.";

    private final PessoaServicoRepository pessoaServicoRepository;
    private final PessoaService pessoaService;
    private final PessoaRepository pessoaRepository;
    private final ServicoService servicoService;
    private final MinisterioService ministerioService;
    private final AutorizacaoService autorizacaoService;

    public VoluntarioService(
            PessoaServicoRepository pessoaServicoRepository,
            PessoaService pessoaService,
            PessoaRepository pessoaRepository,
            ServicoService servicoService,
            MinisterioService ministerioService,
            AutorizacaoService autorizacaoService
    ) {
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.pessoaService = pessoaService;
        this.pessoaRepository = pessoaRepository;
        this.servicoService = servicoService;
        this.ministerioService = ministerioService;
        this.autorizacaoService = autorizacaoService;
    }

    @Transactional(readOnly = true)
    public List<PessoaServicoResponse> listar() {
        List<UUID> pessoaIds = idsPessoasVisiveis();
        if (pessoaIds.isEmpty()) {
            return List.of();
        }
        return toResponses(pessoaServicoRepository.findByPessoaIdIn(pessoaIds));
    }

    @Transactional(readOnly = true)
    public PessoaServicoResponse buscarPorId(UUID id) {
        PessoaServico vinculo = buscarEntidade(id);
        autorizacaoService.assertPodeVerPessoa(pessoaService.buscarEntidade(vinculo.getPessoaId()));
        return toResponse(vinculo);
    }

    @Transactional(readOnly = true)
    public List<PessoaServicoResponse> listarPorPessoa(UUID pessoaId) {
        Pessoa pessoa = pessoaService.buscarEntidade(pessoaId);
        autorizacaoService.assertPodeVerPessoa(pessoa);
        return toResponses(pessoaServicoRepository.findByPessoaId(pessoaId));
    }

    @Transactional(readOnly = true)
    public List<PessoaServicoResponse> listarQualificadosPorServico(UUID servicoId) {
        Servico servico = servicoService.buscarEntidade(servicoId);
        Ministerio ministerio = ministerioService.buscarEntidade(servico.getMinisterioId());
        autorizacaoService.assertAcesso(ministerio.getIgrejaId());
        Set<UUID> pessoaIds = new HashSet<>(idsPessoasVisiveis());
        if (pessoaIds.isEmpty()) {
            return List.of();
        }
        return toResponses(pessoaServicoRepository.findByServicoIdAndAtivoTrue(servicoId).stream()
                .filter(v -> pessoaIds.contains(v.getPessoaId()))
                .toList());
    }

    @Transactional
    public PessoaServicoResponse vincular(PessoaServicoCreateRequest request) {
        Pessoa pessoa = pessoaService.buscarEntidade(request.pessoaId());
        Servico servico = servicoService.buscarEntidade(request.servicoId());
        Ministerio ministerio = ministerioService.buscarEntidade(servico.getMinisterioId());
        autorizacaoService.assertAcesso(pessoa.getIgrejaId());
        autorizacaoService.assertPodeGerenciarMinisterio(ministerio);
        autorizacaoService.assertPessoaDaIgreja(pessoa, ministerio.getIgrejaId());
        assertServicoAceitaVoluntario(servico);

        if (pessoaServicoRepository.existsByPessoaIdAndServicoId(pessoa.getId(), servico.getId())) {
            throw new BusinessException("A pessoa já está vinculada a este serviço");
        }

        if (request.liderResponsavelId() != null) {
            autorizacaoService.assertPessoaDaIgreja(
                    pessoaService.buscarEntidade(request.liderResponsavelId()), pessoa.getIgrejaId()
            );
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
        Servico servicoAtual = servicoService.buscarEntidade(vinculo.getServicoId());
        autorizacaoService.assertPodeGerenciarMinisterio(
                ministerioService.buscarEntidade(servicoAtual.getMinisterioId())
        );

        Pessoa pessoa = pessoaService.buscarEntidade(request.pessoaId());
        Servico servico = servicoService.buscarEntidade(request.servicoId());
        autorizacaoService.assertAcesso(pessoa.getIgrejaId());
        autorizacaoService.assertPodeGerenciarMinisterio(
                ministerioService.buscarEntidade(servico.getMinisterioId())
        );
        autorizacaoService.assertPessoaDaIgreja(
                pessoa,
                ministerioService.buscarEntidade(servico.getMinisterioId()).getIgrejaId()
        );

        boolean trocaServico = !vinculo.getServicoId().equals(servico.getId());
        boolean trocaPessoa = !vinculo.getPessoaId().equals(pessoa.getId());
        // O vínculo do Coordenador (fictício) é da coordenação do ministério: não muda de pessoa nem de serviço.
        if ((trocaServico || trocaPessoa) && Boolean.TRUE.equals(servicoAtual.getFiccao())) {
            throw new BusinessException(MSG_SERVICO_FICTICIO);
        }
        boolean reativa = Boolean.TRUE.equals(request.ativo()) && !Boolean.TRUE.equals(vinculo.getAtivo());
        if (trocaServico || reativa) {
            // Sem troca nem reativação, o vínculo com um serviço que ficou inativo segue editável.
            assertServicoAceitaVoluntario(servico);
        }

        if (!vinculo.getPessoaId().equals(pessoa.getId()) || !vinculo.getServicoId().equals(servico.getId())) {
            pessoaServicoRepository.findByPessoaIdAndServicoId(pessoa.getId(), servico.getId())
                    .filter(outro -> !outro.getId().equals(id))
                    .ifPresent(outro -> {
                        throw new BusinessException("A pessoa já está vinculada a este serviço");
                    });
        }

        if (request.liderResponsavelId() != null) {
            autorizacaoService.assertPessoaDaIgreja(
                    pessoaService.buscarEntidade(request.liderResponsavelId()), pessoa.getIgrejaId()
            );
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
        Servico servico = servicoService.buscarEntidade(vinculo.getServicoId());
        autorizacaoService.assertPodeGerenciarMinisterio(
                ministerioService.buscarEntidade(servico.getMinisterioId())
        );
        pessoaServicoRepository.delete(vinculo);
    }

    /** Chamado depois das checagens de autorização, para não revelar serviços de outra igreja. */
    private static void assertServicoAceitaVoluntario(Servico servico) {
        if (Boolean.TRUE.equals(servico.getFiccao())) {
            throw new BusinessException(MSG_SERVICO_FICTICIO);
        }
        if (!Boolean.TRUE.equals(servico.getAtivo())) {
            throw new BusinessException(MSG_SERVICO_INATIVO);
        }
    }

    @Transactional(readOnly = true)
    public PessoaServico buscarEntidade(UUID id) {
        return pessoaServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo de voluntário não encontrado"));
    }

    private List<UUID> idsPessoasVisiveis() {
        List<UUID> igrejaIds = autorizacaoService.idsParaListagem(null);
        if (igrejaIds.isEmpty()) {
            return List.of();
        }
        return autorizacaoService.filtrarVisiveisNaListagem(pessoaRepository.findByIgrejaIdInOrderByNomeAsc(igrejaIds))
                .stream()
                .map(Pessoa::getId)
                .toList();
    }

    private PessoaServicoResponse toResponse(PessoaServico vinculo) {
        return toResponses(List.of(vinculo)).get(0);
    }

    /** Monta as respostas carregando nomes de pessoas, serviços e ministérios em lote. */
    private List<PessoaServicoResponse> toResponses(List<PessoaServico> vinculos) {
        if (vinculos.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> pessoas = pessoaService.nomesPorId(
                vinculos.stream().map(PessoaServico::getPessoaId).toList());
        Map<UUID, Servico> servicos = servicoService.buscarPorIds(
                vinculos.stream().map(PessoaServico::getServicoId).toList());
        Map<UUID, String> ministerios = ministerioService.nomesPorId(
                servicos.values().stream().map(Servico::getMinisterioId).toList());

        return vinculos.stream().map(vinculo -> {
            Servico servico = servicos.get(vinculo.getServicoId());
            UUID ministerioId = servico == null ? null : servico.getMinisterioId();
            return new PessoaServicoResponse(
                    vinculo.getId(),
                    vinculo.getPessoaId(),
                    pessoas.get(vinculo.getPessoaId()),
                    vinculo.getServicoId(),
                    servico == null ? null : servico.getNome(),
                    ministerioId,
                    ministerioId == null ? null : ministerios.get(ministerioId),
                    vinculo.getCargo(),
                    vinculo.getDataEntrada(),
                    vinculo.getAtivo(),
                    vinculo.getObservacoes(),
                    vinculo.getLiderResponsavelId(),
                    vinculo.getCreatedAt(),
                    vinculo.getUpdatedAt()
            );
        }).toList();
    }
}
