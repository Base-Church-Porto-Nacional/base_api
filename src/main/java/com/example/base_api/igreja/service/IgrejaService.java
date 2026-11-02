package com.example.base_api.igreja.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.common.exception.EntidadeNaoProcessavelException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.igreja.dto.FilialCriadaResponse;
import com.example.base_api.igreja.dto.IgrejaFilialCreateRequest;
import com.example.base_api.igreja.dto.IgrejaResponse;
import com.example.base_api.igreja.dto.IgrejaUpdateRequest;
import com.example.base_api.igreja.dto.PastorFilialRequest;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.entity.IgrejaTipo;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.jornada.service.JornadaService;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.security.Capacidade;
import com.example.base_api.security.Identificador;
import com.example.base_api.security.Papeis;
import com.example.base_api.usuario.dto.LinkAcessoResponse;
import com.example.base_api.usuario.entity.StatusUsuario;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import com.example.base_api.usuario.service.AcessoService;
import com.example.base_api.usuario.service.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class IgrejaService {

    static final String MSG_MODO_PASTOR = "Informe uma pessoa existente ou os dados de uma nova pessoa";
    static final String MSG_SEM_CONTATO = "Cadastre e-mail ou WhatsApp antes de convidar";
    static final String MSG_PESSOA_INATIVA = "Esta pessoa está inativa.";
    static final String MSG_CONTA_DESATIVADA = "A conta desta pessoa está desativada. Reative antes de torná-la pastor.";

    private final IgrejaRepository igrejaRepository;
    private final PessoaRepository pessoaRepository;
    private final UsuarioRepository usuarioRepository;
    private final IgrejaContextoService igrejaContextoService;
    private final AutorizacaoService autorizacaoService;
    private final JornadaService jornadaService;
    private final UsuarioService usuarioService;
    private final AcessoService acessoService;
    private final TransferenciaPessoaService transferenciaPessoaService;
    private final AuditService auditService;

    public IgrejaService(
            IgrejaRepository igrejaRepository,
            PessoaRepository pessoaRepository,
            UsuarioRepository usuarioRepository,
            IgrejaContextoService igrejaContextoService,
            AutorizacaoService autorizacaoService,
            JornadaService jornadaService,
            UsuarioService usuarioService,
            AcessoService acessoService,
            TransferenciaPessoaService transferenciaPessoaService,
            AuditService auditService
    ) {
        this.igrejaRepository = igrejaRepository;
        this.pessoaRepository = pessoaRepository;
        this.usuarioRepository = usuarioRepository;
        this.igrejaContextoService = igrejaContextoService;
        this.autorizacaoService = autorizacaoService;
        this.jornadaService = jornadaService;
        this.usuarioService = usuarioService;
        this.acessoService = acessoService;
        this.transferenciaPessoaService = transferenciaPessoaService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<IgrejaResponse> listarVisiveis() {
        return toResponses(igrejaContextoService.igrejasVisiveis());
    }

    @Transactional(readOnly = true)
    public IgrejaResponse buscarPorId(UUID id) {
        autorizacaoService.assertAcesso(id);
        return toResponse(buscarEntidade(id));
    }

    /**
     * Cria a filial com seu pastor: pessoa existente (transferida) ou nova. Todas as validações rodam antes
     * de salvar qualquer coisa; o pastor recebe PASTOR na filial e, sem conta ativa, um convite.
     */
    @Transactional
    public FilialCriadaResponse criarFilial(UUID matrizId, IgrejaFilialCreateRequest request) {
        autorizacaoService.assertPodeCriarFilial();
        Igreja matriz = buscarEntidade(matrizId);
        if (!matriz.isMatriz()) {
            throw new BusinessException("A igreja informada não é matriz");
        }
        autorizacaoService.assertAcesso(matrizId);

        PastorFilialRequest dados = request.pastor();
        boolean existente = dados.pessoaId() != null;
        boolean nova = StringUtils.hasText(dados.nome());
        if (existente == nova || (existente && (StringUtils.hasText(dados.email()) || StringUtils.hasText(dados.whatsapp())))) {
            throw new BusinessException(MSG_MODO_PASTOR);
        }

        // 1. Resolve o pastor sem salvar nada.
        Pessoa pastor;
        Usuario usuario;
        if (existente) {
            pastor = pessoaRepository.findById(dados.pessoaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada"));
            if (Boolean.FALSE.equals(pastor.getAtivo())) {
                throw new BusinessException(MSG_PESSOA_INATIVA);
            }
            usuario = usuarioRepository.findByPessoaId(pastor.getId()).orElse(null);
        } else {
            pastor = new Pessoa();
            pastor.setNome(dados.nome().trim());
            pastor.setEmail(StringUtils.hasText(dados.email()) ? dados.email().trim().toLowerCase() : null);
            pastor.setWhatsapp(StringUtils.hasText(dados.whatsapp()) ? dados.whatsapp().trim() : null);
            pastor.setDataCadastro(LocalDate.now());
            pastor.setAtivo(true);
            usuario = null;
        }

        boolean precisaConvite = usuario == null || usuario.getStatus() == StatusUsuario.CONVIDADO;
        if (precisaConvite && !temContato(pastor)) {
            throw new EntidadeNaoProcessavelException(MSG_SEM_CONTATO);
        }
        if (precisaConvite) {
            assertIdentificadoresLivres(pastor, usuario);
        }
        if (usuario != null && usuario.getStatus() == StatusUsuario.DESATIVADO) {
            throw new ConflictException(MSG_CONTA_DESATIVADA);
        }
        if (existente) {
            transferenciaPessoaService.validar(pastor);
        }

        // 2. Salva a filial e coloca o pastor nela.
        Igreja filial = new Igreja();
        filial.setNome(request.nome().trim());
        filial.setTipo(IgrejaTipo.FILIAL);
        filial.setIgrejaMatrizId(matriz.getId());
        filial.setAtivo(true);
        Igreja salva = igrejaRepository.save(filial);
        UUID filialId = salva.getId();

        UUID origemId = null;
        if (existente) {
            origemId = transferenciaPessoaService.mover(pastor, filialId);
        } else {
            pastor.setIgrejaId(filialId);
            pastor = pessoaRepository.save(pastor);
            jornadaService.criarJornadaInicial(pastor.getId());
        }
        // Definido só depois da transferência: ela recusa quem já é pastor titular de alguma igreja.
        salva.setPastorPessoaId(pastor.getId());
        salva = igrejaRepository.save(salva);

        // 3. Convite (se ainda sem conta ativa) e papel PASTOR na filial.
        LinkAcessoResponse convite = null;
        if (precisaConvite) {
            convite = acessoService.convidar(pastor.getId());
            UUID pastorId = pastor.getId();
            usuario = usuarioRepository.findByPessoaId(pastorId)
                    .orElseThrow(() -> new IllegalStateException("Conta do pastor não criada: pessoaId=" + pastorId));
        }
        Set<String> papeis = new HashSet<>(usuario.nomesPapeis(filialId));
        papeis.add(Papeis.PASTOR);
        usuarioService.substituirPapeis(usuario, filialId, papeis);

        // Audita depois do flush: os registros são REQUIRES_NEW e não devem sobrar se a criação falhar.
        igrejaRepository.flush();
        if (origemId != null) {
            transferenciaPessoaService.auditar(pastor.getId(), origemId, filialId);
        }
        auditService.registrar(
                "CRIAR_FILIAL",
                "igreja",
                "filialId=" + filialId + " matrizId=" + matrizId + " pastorPessoaId=" + pastor.getId()
                        + " convite=" + (convite != null)
        );
        return new FilialCriadaResponse(toResponse(salva), convite);
    }

    /**
     * Mesma regra do convite (Fase 2): e-mail/telefone da pessoa já usados por OUTRA conta → 409. Checado antes
     * de salvar para que {@code convidar} não falhe depois da filial criada.
     */
    private void assertIdentificadoresLivres(Pessoa pessoa, Usuario propria) {
        UUID propriaContaId = propria == null ? null : propria.getId();
        Identificador.email(pessoa.getEmail())
                .flatMap(usuarioRepository::findByEmailIgnoreCase)
                .filter(outro -> !Objects.equals(outro.getId(), propriaContaId))
                .ifPresent(outro -> {
                    throw new ConflictException("E-mail já usado por outra conta");
                });
        Identificador.telefone(pessoa.getWhatsapp())
                .or(() -> Identificador.telefone(pessoa.getTelefone()))
                .flatMap(usuarioRepository::findByTelefone)
                .filter(outro -> !Objects.equals(outro.getId(), propriaContaId))
                .ifPresent(outro -> {
                    throw new ConflictException("Telefone já usado por outra conta");
                });
    }

    /** Mesmo critério do convite (Fase 2): e-mail, WhatsApp ou telefone válidos. */
    private static boolean temContato(Pessoa pessoa) {
        return Identificador.email(pessoa.getEmail()).isPresent()
                || Identificador.telefone(pessoa.getWhatsapp()).isPresent()
                || Identificador.telefone(pessoa.getTelefone()).isPresent();
    }

    @Transactional
    public IgrejaResponse atualizar(UUID id, IgrejaUpdateRequest request) {
        autorizacaoService.assertGestao(id);
        Igreja igreja = buscarEntidade(id);

        if (!request.pastorPessoaId().equals(igreja.getPastorPessoaId())) {
            // Trocar o pastor da igreja é decisão do pastor (não da secretaria).
            autorizacaoService.assertTem(id, Capacidade.FILIAL_CRIAR);
            Pessoa pastor = pessoaRepository.findById(request.pastorPessoaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pastor não encontrado"));
            if (!id.equals(pastor.getIgrejaId())) {
                throw new BusinessException("A pessoa não pertence a esta igreja");
            }
            igreja.setPastorPessoaId(pastor.getId());
        }

        igreja.setNome(request.nome().trim());
        igreja.setAtivo(request.ativo());
        return toResponse(igrejaRepository.save(igreja));
    }

    @Transactional(readOnly = true)
    public Igreja buscarEntidade(UUID id) {
        return igrejaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Igreja não encontrada"));
    }

    public IgrejaResponse toResponse(Igreja igreja) {
        return toResponses(List.of(igreja)).get(0);
    }

    /** Monta as respostas carregando pastores em lote. */
    public List<IgrejaResponse> toResponses(List<Igreja> igrejas) {
        Map<UUID, String> nomes = pessoaRepository.nomesPorId(
                igrejas.stream().map(Igreja::getPastorPessoaId).toList());

        return igrejas.stream().map(igreja -> {
            return new IgrejaResponse(
                    igreja.getId(),
                    igreja.getNome(),
                    igreja.getTipo(),
                    igreja.getIgrejaMatrizId(),
                    igreja.getPastorPessoaId(),
                    igreja.getPastorPessoaId() == null ? null : nomes.get(igreja.getPastorPessoaId()),
                    igreja.getAtivo(),
                    igreja.getCreatedAt(),
                    igreja.getUpdatedAt()
            );
        }).toList();
    }
}
