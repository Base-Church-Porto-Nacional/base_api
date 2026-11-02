package com.example.base_api.security;

import com.example.base_api.baseministerial.entity.BaseMinisterial;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.service.IgrejaContextoService;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.entity.MinisterioTipo;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.smallgroup.entity.SmallGroup;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * API de autorização usada pelos services. As permissões vêm do {@link PermissaoService}:
 * capacidades por papel (com cascata matriz → filial) e escopos derivados por vínculo
 * (ministérios/bases/grupos liderados).
 */
@Service
public class AutorizacaoService {

    // Removidos na Task 5, junto com a reescrita do UsuarioService.
    public static final Set<String> PAPEIS_GESTAO = Papeis.GESTAO;
    public static final Set<String> PAPEIS_ATALHO = Set.of("MEMBRO", "VOLUNTARIO", "LIDER");

    private final IgrejaContextoService igrejaContextoService;
    private final PermissaoService permissaoService;
    private final MinisterioRepository ministerioRepository;
    private final ServicoRepository servicoRepository;
    private final PessoaServicoRepository pessoaServicoRepository;
    private final EscalaRepository escalaRepository;
    private final EscalaVoluntarioRepository escalaVoluntarioRepository;

    public AutorizacaoService(
            IgrejaContextoService igrejaContextoService,
            PermissaoService permissaoService,
            MinisterioRepository ministerioRepository,
            ServicoRepository servicoRepository,
            PessoaServicoRepository pessoaServicoRepository,
            EscalaRepository escalaRepository,
            EscalaVoluntarioRepository escalaVoluntarioRepository
    ) {
        this.igrejaContextoService = igrejaContextoService;
        this.permissaoService = permissaoService;
        this.ministerioRepository = ministerioRepository;
        this.servicoRepository = servicoRepository;
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.escalaRepository = escalaRepository;
        this.escalaVoluntarioRepository = escalaVoluntarioRepository;
    }

    public Usuario requireUsuario() {
        return igrejaContextoService.requireUsuario();
    }

    public void assertAcesso(UUID igrejaId) {
        igrejaContextoService.assertAcesso(igrejaId);
    }

    public List<UUID> idsParaListagem(UUID igrejaId) {
        return igrejaContextoService.idsParaListagem(igrejaId);
    }

    public UUID igrejaAtualId() {
        return igrejaContextoService.igrejaAtualId();
    }

    public boolean tem(UUID igrejaId, Capacidade capacidade) {
        return igrejaId != null && permissaoService.capacidadesEm(igrejaId).contains(capacidade);
    }

    public void assertTem(UUID igrejaId, Capacidade capacidade) {
        assertTem(igrejaId, capacidade, "Sem permissão para esta ação");
    }

    private void assertTem(UUID igrejaId, Capacidade capacidade, String mensagem) {
        assertAcesso(igrejaId);
        if (!tem(igrejaId, capacidade)) {
            throw new AccessDeniedException(mensagem);
        }
    }

    /** Gestão = pastor ou secretaria (inclusive da matriz): quem vê todas as pessoas da igreja. */
    public boolean isGestao(UUID igrejaId) {
        return tem(igrejaId, Capacidade.PESSOAS_VER_TODAS);
    }

    public void assertGestao(UUID igrejaId) {
        assertTem(igrejaId, Capacidade.PESSOAS_VER_TODAS, "Somente pastor ou secretaria desta igreja");
    }

    /** Pastor ou secretaria da matriz (ações que valem para toda a rede, ex.: gerar escalas do próximo mês). */
    public void assertMatrizGestao() {
        Igreja atual = igrejaContextoService.igrejaAtual();
        if (!atual.isMatriz() || !isGestao(atual.getId())) {
            throw new AccessDeniedException("Somente pastor ou secretaria da matriz");
        }
    }

    public void assertPodeCriarFilial() {
        Igreja atual = igrejaContextoService.igrejaAtual();
        if (!atual.isMatriz() || !tem(atual.getId(), Capacidade.FILIAL_CRIAR)) {
            throw new AccessDeniedException("Somente o pastor da matriz pode criar filial");
        }
    }

    public void assertPodeAtribuirPapeisGestao(UUID igrejaAlvoId) {
        assertTem(igrejaAlvoId, Capacidade.PAPEIS_GESTAO_ATRIBUIR,
                "Somente o pastor da igreja alvo ou da matriz pode atribuir PASTOR/SECRETARIA");
    }

    /** Contagem do culto: pastor/secretaria (inclusive da matriz) ou papel customizado com contagem nesta igreja. */
    public boolean podeLancarContagem(UUID igrejaId) {
        return tem(igrejaId, Capacidade.CULTO_CONTAGEM);
    }

    public void assertPodeLancarContagem(UUID igrejaId) {
        assertTem(igrejaId, Capacidade.CULTO_CONTAGEM,
                "Somente pastor, secretaria ou papel com permissão de contagem desta igreja");
    }

    public void assertPodeMutarPessoas(UUID igrejaId) {
        assertTem(igrejaId, Capacidade.PESSOAS_GERIR);
    }

    public void assertPodeVerPessoa(Pessoa pessoa) {
        assertAcesso(pessoa.getIgrejaId());
        if (!podeIncluirPessoaNaListagem(pessoa)) {
            throw new AccessDeniedException("Sem permissão para ver esta pessoa");
        }
    }

    public boolean podeIncluirPessoaNaListagem(Pessoa pessoa) {
        return !filtrarVisiveisNaListagem(List.of(pessoa)).isEmpty();
    }

    /**
     * Filtra as pessoas que o usuário pode ver numa listagem. A regra é calculada uma vez por igreja:
     * pastor/secretaria vê todas; os demais veem a si mesmos e as pessoas dos ministérios que lideram ou
     * coordenam, diretamente ou pela base (líder, coordenador e voluntários com vínculo ativo).
     */
    public List<Pessoa> filtrarVisiveisNaListagem(List<Pessoa> pessoas) {
        if (pessoas.isEmpty()) {
            return List.of();
        }
        UUID minhaPessoaId = requireUsuario().getPessoaId();
        Map<UUID, Predicate<Pessoa>> regraPorIgreja = new HashMap<>();
        return pessoas.stream()
                .filter(p -> regraPorIgreja
                        .computeIfAbsent(p.getIgrejaId(), igrejaId -> regraDeVisibilidade(igrejaId, minhaPessoaId))
                        .test(p))
                .toList();
    }

    private Predicate<Pessoa> regraDeVisibilidade(UUID igrejaId, UUID minhaPessoaId) {
        if (tem(igrejaId, Capacidade.PESSOAS_VER_TODAS)) {
            return p -> true;
        }
        Set<UUID> visiveis = minhaPessoaId == null
                ? new HashSet<>()
                : pessoasDosMinisterios(permissaoService.ministeriosLideradosEm(igrejaId));
        if (minhaPessoaId != null) {
            visiveis.add(minhaPessoaId);
        }
        return p -> visiveis.contains(p.getId());
    }

    /** Líderes, coordenadores e voluntários ativos dos ministérios informados. */
    private Set<UUID> pessoasDosMinisterios(Set<UUID> ministerioIds) {
        Set<UUID> pessoas = new HashSet<>();
        if (ministerioIds.isEmpty()) {
            return pessoas;
        }
        List<Ministerio> ministerios = ministerioRepository.findAllById(ministerioIds);
        ministerios.forEach(m -> {
            pessoas.add(m.getLiderPessoaId());
            pessoas.add(m.getCoordenadorPessoaId());
        });
        pessoas.remove(null);
        List<UUID> servicoIds = servicoRepository
                .findByMinisterioIdInOrderByNomeAsc(ministerios.stream().map(Ministerio::getId).toList()).stream()
                .map(Servico::getId)
                .toList();
        if (!servicoIds.isEmpty()) {
            pessoaServicoRepository.findByServicoIdInAndAtivoTrue(servicoIds)
                    .forEach(v -> pessoas.add(v.getPessoaId()));
        }
        return pessoas;
    }

    public void assertPessoaDaIgreja(Pessoa pessoa, UUID igrejaId) {
        if (pessoa.getIgrejaId() == null || !pessoa.getIgrejaId().equals(igrejaId)) {
            throw new BusinessException("A pessoa não pertence a esta igreja");
        }
    }

    /** Gestão de ministérios, ou líder/coordenador do ministério ou da sua base. */
    public void assertPodeGerenciarMinisterio(Ministerio ministerio) {
        UUID igrejaId = ministerio.getIgrejaId();
        assertAcesso(igrejaId);
        if (tem(igrejaId, Capacidade.MINISTERIOS_GERIR)
                || permissaoService.ministeriosLideradosEm(igrejaId).contains(ministerio.getId())) {
            return;
        }
        throw new AccessDeniedException("Sem permissão para gerenciar este ministério");
    }

    public void assertPodeCriarMinisterio(UUID igrejaId) {
        assertTem(igrejaId, Capacidade.MINISTERIOS_GERIR);
    }

    public void assertPodeGerenciarBaseMinisterial(BaseMinisterial base) {
        UUID igrejaId = base.getIgrejaId();
        assertAcesso(igrejaId);
        if (tem(igrejaId, Capacidade.MINISTERIOS_GERIR)
                || permissaoService.basesLideradasEm(igrejaId).contains(base.getId())) {
            return;
        }
        throw new AccessDeniedException("Sem permissão para gerenciar esta base ministerial");
    }

    public void assertPodeCriarBaseMinisterial(UUID igrejaId) {
        assertTem(igrejaId, Capacidade.MINISTERIOS_GERIR);
    }

    public void assertPodeCriarSmallGroup(UUID igrejaId) {
        assertTem(igrejaId, Capacidade.MINISTERIOS_GERIR);
    }

    /** Gestão de ministérios, líder do grupo, ou líder/coordenador do ministério do grupo (ou da base dele). */
    public void assertPodeGerenciarSmallGroup(SmallGroup grupo) {
        UUID igrejaId = grupo.getIgrejaId();
        assertAcesso(igrejaId);
        if (tem(igrejaId, Capacidade.MINISTERIOS_GERIR)
                || permissaoService.gruposLideradosEm(igrejaId).contains(grupo.getId())
                || (grupo.getMinisterioId() != null
                        && permissaoService.ministeriosLideradosEm(igrejaId).contains(grupo.getMinisterioId()))) {
            return;
        }
        throw new AccessDeniedException("Sem permissão para gerenciar este pequeno grupo");
    }

    public void assertPodeCriarEscala(TipoEscala tipo, UUID igrejaId, String competencia, UUID ministerioId) {
        assertAcesso(igrejaId);
        if (tem(igrejaId, Capacidade.ESCALAS_GERIR)) {
            return;
        }
        switch (tipo) {
            case SUPERVISOR, PREGADOR ->
                    throw new AccessDeniedException("Somente pastor ou secretaria podem criar escala de supervisor/pregador");
            case CULTO -> {
                if (!isSupervisorNoMes(igrejaId, competencia)) {
                    throw new AccessDeniedException("Somente o supervisor do mês pode criar escala de culto");
                }
            }
            case MINISTERIO -> {
                if (isSupervisorNoMes(igrejaId, competencia)) {
                    return;
                }
                if (ministerioId == null) {
                    throw new AccessDeniedException("Sem permissão para criar esta escala");
                }
                if (!permissaoService.ministeriosLideradosEm(igrejaId).contains(ministerioId)) {
                    throw new AccessDeniedException("Líder/coordenador só pode criar a escala do próprio ministério");
                }
            }
        }
    }

    public void assertPodeMutarEscala(TipoEscala tipo, UUID igrejaId, String competencia, UUID ministerioId) {
        assertPodeCriarEscala(tipo, igrejaId, competencia, ministerioId);
    }

    public boolean isSupervisorNoMes(UUID igrejaId, String competencia) {
        UUID pessoaId = requireUsuario().getPessoaId();
        if (pessoaId == null) {
            return false;
        }
        return escalaRepository.findByIgrejaIdAndCompetenciaAndTipo(igrejaId, competencia, TipoEscala.SUPERVISOR)
                .stream()
                .flatMap(e -> escalaVoluntarioRepository.findByEscalaId(e.getId()).stream())
                .anyMatch(i -> pessoaId.equals(i.getPessoaId()));
    }

    public boolean podeVerEscala(Escala escala) {
        if (isGestao(escala.getIgrejaId())) {
            return true;
        }
        UUID pessoaId = requireUsuario().getPessoaId();
        if (pessoaId == null) {
            return false;
        }
        boolean designado = escalaVoluntarioRepository.findByEscalaId(escala.getId()).stream()
                .anyMatch(i -> pessoaId.equals(i.getPessoaId()));
        if (designado) {
            return true;
        }
        return escala.getTipo() == TipoEscala.MINISTERIO && escala.getMinisterioId() != null
                && permissaoService.ministeriosLideradosEm(escala.getIgrejaId()).contains(escala.getMinisterioId());
    }

    /** Pastor/secretaria (vê tudo), quem edita texto ou louvor, ou quem pertence à mídia. */
    public void assertPodeVerConteudo(UUID igrejaId, UUID pregadorPessoaId) {
        assertAcesso(igrejaId);
        if (tem(igrejaId, Capacidade.PESSOAS_VER_TODAS) || podeEditarTexto(igrejaId, pregadorPessoaId)
                || podeEditarLouvor(igrejaId) || pertenceAMidia(igrejaId)) {
            return;
        }
        throw new AccessDeniedException("Sem permissão para ver o conteúdo deste culto");
    }

    /** Pastor (override) ou o próprio pregador. */
    public boolean podeEditarTexto(UUID igrejaId, UUID pregadorPessoaId) {
        if (tem(igrejaId, Capacidade.CULTO_CONTEUDO_EDITAR)) {
            return true;
        }
        UUID pessoaId = requireUsuario().getPessoaId();
        return pessoaId != null && pessoaId.equals(pregadorPessoaId);
    }

    /** Pastor (override) ou líder/coordenador (direto ou pela base) de um ministério de música. */
    public boolean podeEditarLouvor(UUID igrejaId) {
        if (tem(igrejaId, Capacidade.CULTO_CONTEUDO_EDITAR)) {
            return true;
        }
        Set<UUID> liderados = permissaoService.ministeriosLideradosEm(igrejaId);
        return !liderados.isEmpty() && ministerioRepository.findAllById(liderados).stream()
                .anyMatch(m -> m.getTipo() == MinisterioTipo.MUSICA);
    }

    public boolean pertenceAMidia(UUID igrejaId) {
        UUID pessoaId = requireUsuario().getPessoaId();
        if (pessoaId == null) {
            return false;
        }
        List<Ministerio> midias = ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId).stream()
                .filter(m -> m.getTipo() == MinisterioTipo.MIDIA)
                .toList();
        for (Ministerio m : midias) {
            if (pessoaId.equals(m.getLiderPessoaId()) || pessoaId.equals(m.getCoordenadorPessoaId())) {
                return true;
            }
            List<UUID> servicoIds = servicoRepository.findByMinisterioIdOrderByNomeAsc(m.getId()).stream()
                    .filter(s -> !Boolean.TRUE.equals(s.getFiccao()))
                    .map(Servico::getId)
                    .toList();
            if (!servicoIds.isEmpty()
                    && pessoaServicoRepository.findByServicoIdInAndAtivoTrue(servicoIds).stream()
                    .anyMatch(v -> pessoaId.equals(v.getPessoaId()))) {
                return true;
            }
        }
        return false;
    }

    public void assertPodeAcessarDisponibilidade(Pessoa pessoa) {
        assertAcesso(pessoa.getIgrejaId());
        Usuario usuario = requireUsuario();
        if (isGestao(pessoa.getIgrejaId())) {
            return;
        }
        if (usuario.getPessoaId() == null || !usuario.getPessoaId().equals(pessoa.getId())) {
            throw new AccessDeniedException("Só é possível gerir a própria disponibilidade");
        }
    }

    public void assertPodeCriarUsuario(UUID igrejaId) {
        assertTem(igrejaId, Capacidade.CONTAS_GERIR);
    }

    public Set<String> validarPapeisAtalho(Set<String> papeis) {
        if (papeis == null || papeis.isEmpty()) {
            throw new AccessDeniedException("Informe ao menos um papel");
        }
        Set<String> normalizados = new HashSet<>();
        for (String nome : papeis) {
            String papel = nome.toUpperCase();
            if (PAPEIS_GESTAO.contains(papel)) {
                throw new AccessDeniedException(
                        "Não é permitido atribuir PASTOR/SECRETARIA neste atalho. Use o endpoint de promoção."
                );
            }
            if (!PAPEIS_ATALHO.contains(papel)) {
                throw new AccessDeniedException("Papel não permitido neste atalho: " + papel);
            }
            normalizados.add(papel);
        }
        return normalizados;
    }

    public void assertPessoaSemUsuario(boolean jaTemUsuario) {
        if (jaTemUsuario) {
            throw new ConflictException("Esta pessoa já possui usuário vinculado");
        }
    }
}
