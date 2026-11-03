package com.example.base_api.pessoa.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.jornada.entity.JornadaMembro;
import com.example.base_api.jornada.service.JornadaService;
import com.example.base_api.pessoa.dto.PessoaCreateRequest;
import com.example.base_api.pessoa.dto.PessoaResponse;
import com.example.base_api.pessoa.dto.PessoaResumoResponse;
import com.example.base_api.pessoa.dto.PessoaUpdateRequest;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.security.Capacidade;
import com.example.base_api.security.Identificador;
import com.example.base_api.security.Papeis;
import com.example.base_api.usuario.service.LiderService;
import com.example.base_api.usuario.entity.StatusUsuario;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.base_api.common.util.Textos.blankToNull;

@Service
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final JornadaService jornadaService;
    private final AutorizacaoService autorizacaoService;
    private final UsuarioRepository usuarioRepository;
    private final PessoaServicoRepository pessoaServicoRepository;
    private final LiderService liderService;
    private final AuditService auditService;

    private static final String FUNCAO_LIDER = "LIDER";
    private static final String FUNCAO_VOLUNTARIO = "VOLUNTARIO";
    private static final String FUNCAO_MEMBRO = "MEMBRO";

    public PessoaService(
            PessoaRepository pessoaRepository,
            JornadaService jornadaService,
            AutorizacaoService autorizacaoService,
            UsuarioRepository usuarioRepository,
            PessoaServicoRepository pessoaServicoRepository,
            LiderService liderService,
            AuditService auditService
    ) {
        this.liderService = liderService;
        this.auditService = auditService;
        this.pessoaRepository = pessoaRepository;
        this.jornadaService = jornadaService;
        this.autorizacaoService = autorizacaoService;
        this.usuarioRepository = usuarioRepository;
        this.pessoaServicoRepository = pessoaServicoRepository;
    }

    @Transactional(readOnly = true)
    public List<PessoaResponse> listar(UUID igrejaId) {
        List<UUID> ids = autorizacaoService.idsParaListagem(igrejaId);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Pessoa> pessoas = autorizacaoService.filtrarVisiveisNaListagem(
                pessoaRepository.findByIgrejaIdInOrderByNomeAsc(ids));
        if (pessoas.isEmpty()) {
            return List.of();
        }
        Map<UUID, Usuario> usuarios = usuarioRepository
                .findByPessoaIdIn(pessoas.stream().map(Pessoa::getId).toList()).stream()
                .collect(Collectors.toMap(Usuario::getPessoaId, u -> u, (a, b) -> a));
        Map<UUID, JornadaMembro> jornadas = jornadaService.buscarPorPessoas(
                pessoas.stream().map(Pessoa::getId).toList());
        Map<UUID, Set<String>> funcoes = funcoesPorPessoa(pessoas, usuarios);
        return pessoas.stream()
                .map(p -> toResponse(p, usuarios.get(p.getId()), jornadas.get(p.getId()), funcoes.get(p.getId())))
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
        autorizacaoService.assertPodeCadastrarPessoa(igrejaId, request.servicoId());
        boolean podeEditarPastoral = autorizacaoService.tem(igrejaId, Capacidade.DADOS_PASTORAIS_EDITAR);
        if (!podeEditarPastoral && blankToNull(request.observacoes()) != null) {
            throw new AccessDeniedException("Somente pastor ou secretaria alteram dados pastorais");
        }

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
        if (request.servicoId() != null) {
            PessoaServico vinculo = new PessoaServico();
            vinculo.setPessoaId(salva.getId());
            vinculo.setServicoId(request.servicoId());
            vinculo.setAtivo(true);
            pessoaServicoRepository.save(vinculo);
        }
        return toResponse(salva);
    }

    /** Atualização parcial: campo nulo não altera; texto vazio limpa (exceto nome). */
    @Transactional
    public PessoaResponse atualizar(UUID id, PessoaUpdateRequest request) {
        Pessoa pessoa = buscarEntidade(id);
        autorizacaoService.assertPodeEditarContato(pessoa);
        boolean alteraAtivo = request.ativo() != null && !Objects.equals(request.ativo(), pessoa.getAtivo());
        boolean alteraObservacoes = request.observacoes() != null
                && !Objects.equals(blankToNull(request.observacoes()), blankToNull(pessoa.getObservacoes()));
        if ((alteraAtivo || alteraObservacoes)
                && !autorizacaoService.tem(pessoa.getIgrejaId(), Capacidade.DADOS_PASTORAIS_EDITAR)) {
            throw new AccessDeniedException("Somente pastor ou secretaria alteram dados pastorais");
        }
        if (request.nome() != null && request.nome().isBlank()) {
            throw new BusinessException("Nome é obrigatório");
        }

        if (request.email() != null && !request.email().isBlank()
                && pessoaRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new BusinessException("Já existe uma pessoa com este e-mail");
        }

        String emailAntes = Identificador.email(pessoa.getEmail()).orElse(null);
        String telefoneAntes = telefoneDeLogin(pessoa);

        if (request.nome() != null) {
            pessoa.setNome(request.nome().trim());
        }
        if (request.nomeSocial() != null) {
            pessoa.setNomeSocial(blankToNull(request.nomeSocial()));
        }
        if (request.email() != null) {
            pessoa.setEmail(blankToNull(request.email()));
        }
        if (request.telefone() != null) {
            pessoa.setTelefone(blankToNull(request.telefone()));
        }
        if (request.whatsapp() != null) {
            pessoa.setWhatsapp(blankToNull(request.whatsapp()));
        }
        if (request.dataNascimento() != null) {
            pessoa.setDataNascimento(request.dataNascimento());
        }
        if (alteraAtivo) {
            pessoa.setAtivo(request.ativo());
        }
        if (alteraObservacoes) {
            pessoa.setObservacoes(blankToNull(request.observacoes()));
        }
        if (request.email() != null || request.telefone() != null || request.whatsapp() != null) {
            sincronizarLoginDaConta(pessoa, emailAntes, telefoneAntes);
        }

        return toResponse(pessoaRepository.save(pessoa));
    }

    /**
     * Leva e-mail (minúsculo) e telefone (WhatsApp normalizado, senão o telefone) da pessoa para a conta.
     * Só quem gere contas na igreja — e, se a conta é de pastor/secretaria, só quem atribui esses papéis.
     * Só leva o identificador que mudou NESTA requisição (comparado ao valor anterior da pessoa), para que um número
     * plantado antes por um líder não vá para o login num save posterior da secretaria.
     * Um líder editando o contato do seu voluntário NÃO troca o login dele (seria tomada de conta).
     */
    private void sincronizarLoginDaConta(Pessoa pessoa, String emailAntes, String telefoneAntes) {
        Usuario usuario = usuarioRepository.findByPessoaId(pessoa.getId()).orElse(null);
        if (usuario == null) {
            return;
        }
        UUID igrejaId = usuario.getIgrejaId() != null ? usuario.getIgrejaId() : pessoa.getIgrejaId();
        if (!autorizacaoService.tem(igrejaId, Capacidade.CONTAS_GERIR)) {
            return;
        }
        boolean contaDeGestao = usuario.nomesPapeisSistema(igrejaId).stream().anyMatch(Papeis.GESTAO::contains);
        if (contaDeGestao && !autorizacaoService.tem(igrejaId, Capacidade.PAPEIS_GESTAO_ATRIBUIR)) {
            return;
        }

        String email = Identificador.email(pessoa.getEmail()).orElse(null);
        String telefone = telefoneDeLogin(pessoa);
        boolean emailMudou = !Objects.equals(email, emailAntes) && !Objects.equals(email, usuario.getEmail());
        boolean telefoneMudou = !Objects.equals(telefone, telefoneAntes) && !Objects.equals(telefone, usuario.getTelefone());
        if (!emailMudou && !telefoneMudou) {
            return;
        }
        String emailFinal = emailMudou ? email : usuario.getEmail();
        String telefoneFinal = telefoneMudou ? telefone : usuario.getTelefone();
        if (emailFinal == null && telefoneFinal == null) {
            throw new BusinessException("Informe e-mail ou telefone para entrar no app");
        }
        if (email != null && emailMudou) {
            usuarioRepository.findByEmailIgnoreCase(email)
                    .filter(outro -> !outro.getId().equals(usuario.getId()))
                    .ifPresent(outro -> {
                        throw new ConflictException("E-mail já usado por outra conta");
                    });
        }
        if (telefone != null && telefoneMudou) {
            usuarioRepository.findByTelefone(telefone)
                    .filter(outro -> !outro.getId().equals(usuario.getId()))
                    .ifPresent(outro -> {
                        throw new ConflictException("Telefone já usado por outra conta");
                    });
        }
        usuario.setEmail(emailFinal);
        usuario.setTelefone(telefoneFinal);
        usuarioRepository.save(usuario);
        auditService.registrar("SINCRONIZAR_LOGIN", "usuario", "usuarioId=" + usuario.getId()
                + " email=" + emailMudou + " telefone=" + telefoneMudou);
    }

    private static String telefoneDeLogin(Pessoa pessoa) {
        return Identificador.telefone(pessoa.getWhatsapp())
                .or(() -> Identificador.telefone(pessoa.getTelefone()))
                .orElse(null);
    }

    /**
     * Conta CONVIDADO/DESATIVADO sai junto com a pessoa (papéis e tokens caem em cascata no banco);
     * com acesso ATIVO é preciso desativar antes.
     */
    @Transactional
    public void excluir(UUID id) {
        Pessoa pessoa = buscarEntidade(id);
        autorizacaoService.assertTem(pessoa.getIgrejaId(), Capacidade.PESSOAS_GERIR);
        Usuario usuario = usuarioRepository.findByPessoaId(id).orElse(null);
        if (usuario != null) {
            if (usuario.getStatus() == StatusUsuario.ATIVO) {
                throw new ConflictException("Esta pessoa tem acesso ativo ao app. Desative o acesso antes de excluir.");
            }
            UUID igrejaConta = usuario.getIgrejaId() != null ? usuario.getIgrejaId() : pessoa.getIgrejaId();
            if (usuario.nomesPapeisSistema(igrejaConta).stream().anyMatch(Papeis.GESTAO::contains)) {
                autorizacaoService.assertPodeAtribuirPapeisGestao(igrejaConta);
            }
            usuarioRepository.delete(usuario);
            usuarioRepository.flush();
        }
        pessoaRepository.delete(pessoa);
        if (usuario != null) {
            // Audita depois do flush: o registro é REQUIRES_NEW e não deve sobrar se a exclusão falhar.
            pessoaRepository.flush();
            auditService.registrar("EXCLUIR_USUARIO", "usuario",
                    "usuarioId=" + usuario.getId() + " pessoaId=" + id + " status=" + usuario.getStatus());
        }
    }

    @Transactional(readOnly = true)
    public List<PessoaResumoResponse> buscarPorNome(String termo) {
        String nome = termo == null ? "" : termo.trim();
        if (nome.length() < 2) {
            throw new BusinessException("Informe ao menos 2 caracteres para buscar");
        }
        List<UUID> igrejaIds = autorizacaoService.igrejasParaBuscaDePessoas();
        autorizacaoService.assertPodeBuscarPessoas(igrejaIds);
        return pessoaRepository
                .findTop20ByIgrejaIdInAndAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(igrejaIds, nome).stream()
                .limit(20)
                .map(p -> new PessoaResumoResponse(p.getId(), p.getNome()))
                .toList();
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
        Usuario usuario = usuarioRepository.findByPessoaId(pessoa.getId()).orElse(null);
        Map<UUID, Usuario> usuarios = usuario == null ? Map.of() : Map.of(pessoa.getId(), usuario);
        return toResponse(
                pessoa,
                usuario,
                jornadaService.buscarPorPessoas(List.of(pessoa.getId())).get(pessoa.getId()),
                funcoesPorPessoa(List.of(pessoa), usuarios).get(pessoa.getId())
        );
    }

    /**
     * Funções derivadas por pessoa, em lote (consultas fixas, sem N+1): PASTOR/SECRETARIA = papéis de
     * sistema do usuário na igreja da pessoa; LIDER = lidera/coordena ministério ou base ativos, ou lidera
     * grupo; VOLUNTARIO = vínculo ativo em serviço; MEMBRO = tem usuário ativo.
     */
    private Map<UUID, Set<String>> funcoesPorPessoa(List<Pessoa> pessoas, Map<UUID, Usuario> usuarios) {
        List<UUID> pessoaIds = pessoas.stream().map(Pessoa::getId).toList();
        List<UUID> igrejaIds = pessoas.stream().map(Pessoa::getIgrejaId)
                .filter(Objects::nonNull).distinct().toList();
        Set<UUID> lideres = liderService.pessoaIdsLideres(igrejaIds);
        Set<UUID> voluntarios = new HashSet<>(pessoaServicoRepository.pessoaIdsComVinculoAtivo(pessoaIds));
        Map<UUID, Set<String>> resultado = new HashMap<>();
        for (Pessoa p : pessoas) {
            Set<String> funcoes = new TreeSet<>();
            Usuario usuario = usuarios.get(p.getId());
            if (usuario != null) {
                usuario.nomesPapeisSistema(p.getIgrejaId()).stream()
                        .filter(Papeis.GESTAO::contains)
                        .forEach(funcoes::add);
                if (usuario.isAtivo()) {
                    funcoes.add(FUNCAO_MEMBRO);
                }
            }
            if (lideres != null && lideres.contains(p.getId())) {
                funcoes.add(FUNCAO_LIDER);
            }
            if (voluntarios.contains(p.getId())) {
                funcoes.add(FUNCAO_VOLUNTARIO);
            }
            resultado.put(p.getId(), funcoes);
        }
        return resultado;
    }

    private PessoaResponse toResponse(Pessoa pessoa, Usuario usuario, JornadaMembro jornada, Set<String> funcoes) {
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
                pessoa.getUpdatedAt(),
                jornada != null ? jornada.getAceitouJesus() : null,
                jornada != null ? jornada.getBatizado() : null,
                jornada != null ? jornada.getConcluiuTrilhaBase() : null,
                funcoes != null ? funcoes : Set.of(),
                usuario != null ? usuario.getStatus() : null
        );
    }
}
