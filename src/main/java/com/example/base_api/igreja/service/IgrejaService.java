package com.example.base_api.igreja.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.common.exception.EntidadeNaoProcessavelException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.igreja.dto.FilialCriadaResponse;
import com.example.base_api.igreja.dto.PastorTrocadoResponse;
import com.example.base_api.igreja.dto.IgrejaFilialCreateRequest;
import com.example.base_api.igreja.dto.IgrejaResponse;
import com.example.base_api.igreja.dto.IgrejaUpdateRequest;
import com.example.base_api.igreja.dto.PastorFilialRequest;
import com.example.base_api.igreja.dto.TrocarPastorRequest;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.entity.IgrejaTipo;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.jornada.service.JornadaService;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
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
        if (existente && dados.pessoaId().equals(igrejaContextoService.requireUsuario().getPessoaId())) {
            throw new ConflictException("Você não pode se transferir para a nova filial.");
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

        boolean precisaConvite = validarContaDoPastor(pastor, usuario);
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
        ContaPastor conta = garantirContaEPastor(pastor, usuario, precisaConvite, filialId);
        LinkAcessoResponse convite = conta.convite();

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
     * Pré-validações da conta do futuro pastor (antes de salvar qualquer coisa). Devolve se ele precisa de
     * convite: sem conta ou ainda CONVIDADO.
     */
    private boolean validarContaDoPastor(Pessoa pastor, Usuario usuario) {
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
        return precisaConvite;
    }

    private record ContaPastor(Usuario usuario, LinkAcessoResponse convite) {
    }

    /** Convida (se preciso) e garante PASTOR de sistema na igreja, preservando os demais papéis da conta. */
    private ContaPastor garantirContaEPastor(Pessoa pastor, Usuario usuario, boolean precisaConvite, UUID igrejaId) {
        LinkAcessoResponse convite = null;
        if (precisaConvite) {
            convite = acessoService.convidar(pastor.getId());
            UUID pastorId = pastor.getId();
            usuario = usuarioRepository.findByPessoaId(pastorId)
                    .orElseThrow(() -> new IllegalStateException("Conta do pastor não criada: pessoaId=" + pastorId));
        }
        Set<String> papeis = new HashSet<>(usuario.nomesPapeis(igrejaId));
        papeis.add(Papeis.PASTOR);
        usuarioService.substituirPapeis(usuario, igrejaId, papeis);
        return new ContaPastor(usuario, convite);
    }

    /**
     * Troca o pastor titular: o novo recebe PASTOR (e convite, se sem conta ativa) e o anterior perde o papel, mantendo
     * os demais. Só que, se o novo ainda não está ATIVO (convidado/recém-convidado), tirar o PASTOR do anterior
     * deixaria a igreja sem nenhum pastor ativo; nesse caso o anterior mantém o PASTOR, só o titular
     * ({@code pastorPessoaId}) muda e a resposta sai com {@code anteriorMantevePastor = true}.
     */
    @Transactional
    public PastorTrocadoResponse trocarPastor(UUID igrejaId, TrocarPastorRequest request) {
        Igreja igreja = buscarEntidade(igrejaId);
        autorizacaoService.assertPodeTrocarPastor(igreja);

        UUID anteriorId = igreja.getPastorPessoaId();
        if (request.pessoaId().equals(anteriorId)) {
            return new PastorTrocadoResponse(toResponse(igreja), null, false);
        }

        Pessoa novo = pessoaRepository.findById(request.pessoaId())
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada"));
        if (!igrejaId.equals(novo.getIgrejaId())) {
            throw new BusinessException("A pessoa não pertence a esta igreja");
        }
        if (Boolean.FALSE.equals(novo.getAtivo())) {
            throw new BusinessException(MSG_PESSOA_INATIVA);
        }
        Usuario usuarioNovo = usuarioRepository.findByPessoaId(novo.getId()).orElse(null);
        boolean precisaConvite = validarContaDoPastor(novo, usuarioNovo);

        igreja.setPastorPessoaId(novo.getId());
        Igreja salva = igrejaRepository.save(igreja);
        ContaPastor conta = garantirContaEPastor(novo, usuarioNovo, precisaConvite, igrejaId);

        boolean anteriorMantevePastor = false;
        Usuario anterior = anteriorId == null ? null : usuarioRepository.findByPessoaId(anteriorId).orElse(null);
        if (anterior != null) {
            Set<String> restantes = new HashSet<>(anterior.nomesPapeis(igrejaId));
            if (restantes.contains(Papeis.PASTOR)) {
                if (deixariaIgrejaSemPastorAtivo(anterior, igrejaId)) {
                    anteriorMantevePastor = true;
                } else {
                    restantes.remove(Papeis.PASTOR);
                    usuarioService.substituirPapeis(anterior, igrejaId, restantes);
                    // Só invalida quando o papel de fato saiu; se ele ficou, os tokens atuais continuam válidos.
                    anterior.invalidarTokens();
                    usuarioRepository.save(anterior);
                }
            }
        }

        // Audita no fim: os registros são REQUIRES_NEW e não devem sobrar se algo acima falhar.
        igrejaRepository.flush();
        auditService.registrar(
                "TROCAR_PASTOR",
                "igreja",
                "igrejaId=" + igrejaId + " anterior=" + anteriorId + " novo=" + novo.getId()
                        + " convite=" + (conta.convite() != null)
        );
        return new PastorTrocadoResponse(toResponse(salva), conta.convite(), anteriorMantevePastor);
    }

    /** True se, sem {@code anterior}, não restaria nenhum usuário ATIVO com o PASTOR do sistema na igreja. */
    private boolean deixariaIgrejaSemPastorAtivo(Usuario anterior, UUID igrejaId) {
        // Flush explícito: o PASTOR do novo acabou de ser gravado via substituirPapeis e precisa entrar na contagem
        // (não depender do auto-flush do Hibernate antes da consulta JPQL). Mesmo EntityManager dos repositórios.
        usuarioRepository.flush();
        long ativos = usuarioRepository.countByPapelAndIgrejaId(Papeis.PASTOR, igrejaId);
        long semAnterior = anterior.isAtivo() ? ativos - 1 : ativos;
        return semAnterior < 1;
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
