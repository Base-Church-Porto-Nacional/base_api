package com.example.base_api.security;

import com.example.base_api.baseministerial.entity.BaseMinisterial;
import com.example.base_api.baseministerial.repository.BaseMinisterialRepository;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AutorizacaoService {

    public static final Set<String> PAPEIS_GESTAO = Set.of("ADMIN", "PASTOR");
    public static final Set<String> PAPEIS_ATALHO = Set.of("MEMBRO", "VOLUNTARIO", "LIDER");

    private final IgrejaContextoService igrejaContextoService;
    private final MinisterioRepository ministerioRepository;
    private final ServicoRepository servicoRepository;
    private final PessoaServicoRepository pessoaServicoRepository;
    private final EscalaRepository escalaRepository;
    private final EscalaVoluntarioRepository escalaVoluntarioRepository;
    private final BaseMinisterialRepository baseMinisterialRepository;

    public AutorizacaoService(
            IgrejaContextoService igrejaContextoService,
            MinisterioRepository ministerioRepository,
            ServicoRepository servicoRepository,
            PessoaServicoRepository pessoaServicoRepository,
            EscalaRepository escalaRepository,
            EscalaVoluntarioRepository escalaVoluntarioRepository,
            BaseMinisterialRepository baseMinisterialRepository
    ) {
        this.igrejaContextoService = igrejaContextoService;
        this.ministerioRepository = ministerioRepository;
        this.servicoRepository = servicoRepository;
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.escalaRepository = escalaRepository;
        this.escalaVoluntarioRepository = escalaVoluntarioRepository;
        this.baseMinisterialRepository = baseMinisterialRepository;
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

    public void assertPastorOuAdmin(UUID igrejaId) {
        igrejaContextoService.assertPastorOuAdmin(igrejaId);
    }

    public void assertMatrizPastorOuAdmin() {
        igrejaContextoService.assertMatrizPastorOuAdmin();
    }

    public boolean isPastorOuAdmin(UUID igrejaId) {
        return igrejaContextoService.isPastorOuAdmin(igrejaId);
    }

    public void assertPodeMutarPessoas(UUID igrejaId) {
        assertPastorOuAdmin(igrejaId);
    }

    public void assertPodeVerPessoa(Pessoa pessoa) {
        assertAcesso(pessoa.getIgrejaId());
        if (!podeIncluirPessoaNaListagem(pessoa)) {
            throw new AccessDeniedException("Sem permissão para ver esta pessoa");
        }
    }

    public boolean podeListarPessoasDaIgreja(UUID igrejaId) {
        return isPastorOuAdmin(igrejaId);
    }

    public boolean podeIncluirPessoaNaListagem(Pessoa pessoa) {
        if (isPastorOuAdmin(pessoa.getIgrejaId())) {
            return true;
        }
        Usuario usuario = requireUsuario();
        if (usuario.getPessoaId() != null && usuario.getPessoaId().equals(pessoa.getId())) {
            return true;
        }
        return pessoaNoMinisterioDoUsuario(pessoa);
    }

    public void assertPessoaDaIgreja(Pessoa pessoa, UUID igrejaId) {
        if (pessoa.getIgrejaId() == null || !pessoa.getIgrejaId().equals(igrejaId)) {
            throw new BusinessException("A pessoa não pertence a esta igreja");
        }
    }

    public void assertPodeGerenciarMinisterio(Ministerio ministerio) {
        assertAcesso(ministerio.getIgrejaId());
        if (isPastorOuAdmin(ministerio.getIgrejaId())) {
            return;
        }
        UUID pessoaId = requireUsuario().getPessoaId();
        if (pessoaId != null && (pessoaId.equals(ministerio.getLiderPessoaId())
                || pessoaId.equals(ministerio.getCoordenadorPessoaId()))) {
            return;
        }
        if (pessoaId != null && ministerio.getBaseMinisterialId() != null) {
            BaseMinisterial base = baseMinisterialRepository.findById(ministerio.getBaseMinisterialId())
                    .orElse(null);
            if (base != null && (pessoaId.equals(base.getLiderPessoaId())
                    || pessoaId.equals(base.getCoordenadorPessoaId()))) {
                return;
            }
        }
        throw new AccessDeniedException("Sem permissão para gerenciar este ministério");
    }

    public void assertPodeCriarMinisterio(UUID igrejaId) {
        assertPastorOuAdmin(igrejaId);
    }

    public void assertPodeGerenciarBaseMinisterial(BaseMinisterial base) {
        assertAcesso(base.getIgrejaId());
        if (isPastorOuAdmin(base.getIgrejaId())) {
            return;
        }
        UUID pessoaId = requireUsuario().getPessoaId();
        if (pessoaId != null && (pessoaId.equals(base.getLiderPessoaId())
                || pessoaId.equals(base.getCoordenadorPessoaId()))) {
            return;
        }
        throw new AccessDeniedException("Sem permissão para gerenciar esta base ministerial");
    }

    public void assertPodeCriarBaseMinisterial(UUID igrejaId) {
        assertPastorOuAdmin(igrejaId);
    }

    public void assertPodeCriarSmallGroup(UUID igrejaId) {
        assertPastorOuAdmin(igrejaId);
    }

    public void assertPodeGerenciarSmallGroup(SmallGroup grupo) {
        assertAcesso(grupo.getIgrejaId());
        if (isPastorOuAdmin(grupo.getIgrejaId())) {
            return;
        }
        UUID pessoaId = requireUsuario().getPessoaId();
        if (pessoaId == null) {
            throw new AccessDeniedException("Sem permissão para gerenciar este pequeno grupo");
        }
        if (pessoaId.equals(grupo.getLiderPessoaId())) {
            return;
        }
        if (grupo.getMinisterioId() != null) {
            Ministerio ministerio = ministerioRepository.findById(grupo.getMinisterioId()).orElse(null);
            if (ministerio != null) {
                if (pessoaId.equals(ministerio.getLiderPessoaId())
                        || pessoaId.equals(ministerio.getCoordenadorPessoaId())) {
                    return;
                }
                if (ministerio.getBaseMinisterialId() != null) {
                    BaseMinisterial base = baseMinisterialRepository
                            .findById(ministerio.getBaseMinisterialId())
                            .orElse(null);
                    if (base != null && (pessoaId.equals(base.getLiderPessoaId())
                            || pessoaId.equals(base.getCoordenadorPessoaId()))) {
                        return;
                    }
                }
            }
        }
        throw new AccessDeniedException("Sem permissão para gerenciar este pequeno grupo");
    }

    public void assertPodeCriarEscala(TipoEscala tipo, UUID igrejaId, String competencia, UUID ministerioId) {
        igrejaContextoService.requireUsuario();
        assertAcesso(igrejaId);
        if (isPastorOuAdmin(igrejaId)) {
            return;
        }
        boolean supervisor = isSupervisorNoMes(igrejaId, competencia);
        UUID pessoaId = requireUsuario().getPessoaId();

        switch (tipo) {
            case SUPERVISOR, PREGADOR ->
                    throw new AccessDeniedException("Somente pastor ou admin podem criar escala de supervisor/pregador");
            case CULTO -> {
                if (!supervisor) {
                    throw new AccessDeniedException("Somente o supervisor do mês pode criar escala de culto");
                }
            }
            case MINISTERIO -> {
                if (supervisor) {
                    return;
                }
                if (ministerioId == null) {
                    throw new AccessDeniedException("Sem permissão para criar esta escala");
                }
                Ministerio ministerio = ministerioRepository.findById(ministerioId)
                        .orElseThrow(() -> new AccessDeniedException("Ministério não encontrado"));
                if (pessoaId == null || (!pessoaId.equals(ministerio.getLiderPessoaId())
                        && !pessoaId.equals(ministerio.getCoordenadorPessoaId()))) {
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

    public boolean podeVerTodasEscalasDaIgreja(UUID igrejaId) {
        return isPastorOuAdmin(igrejaId);
    }

    public boolean podeVerEscala(Escala escala) {
        if (isPastorOuAdmin(escala.getIgrejaId())) {
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
        if (escala.getTipo() == TipoEscala.MINISTERIO && escala.getMinisterioId() != null) {
            return ministerioRepository.findById(escala.getMinisterioId())
                    .filter(m -> pessoaId.equals(m.getLiderPessoaId()) || pessoaId.equals(m.getCoordenadorPessoaId()))
                    .isPresent();
        }
        return false;
    }

    private boolean pessoaNoMinisterioDoUsuario(Pessoa pessoa) {
        UUID pessoaId = requireUsuario().getPessoaId();
        if (pessoaId == null) {
            return false;
        }
        List<Ministerio> meus = ministerioRepository.findByIgrejaIdOrderByNomeAsc(pessoa.getIgrejaId()).stream()
                .filter(m -> pessoaId.equals(m.getLiderPessoaId()) || pessoaId.equals(m.getCoordenadorPessoaId()))
                .toList();
        if (meus.isEmpty()) {
            return false;
        }
        if (meus.stream().anyMatch(m -> pessoa.getId().equals(m.getLiderPessoaId())
                || pessoa.getId().equals(m.getCoordenadorPessoaId()))) {
            return true;
        }
        List<UUID> servicoIds = meus.stream()
                .flatMap(m -> servicoRepository.findByMinisterioIdOrderByNomeAsc(m.getId()).stream())
                .map(Servico::getId)
                .toList();
        if (servicoIds.isEmpty()) {
            return false;
        }
        return pessoaServicoRepository.findByServicoIdInAndAtivoTrue(servicoIds).stream()
                .anyMatch(v -> pessoa.getId().equals(v.getPessoaId()));
    }

    public void assertPodeVerConteudo(UUID igrejaId, UUID pregadorPessoaId) {
        assertAcesso(igrejaId);
        if (isPastorOuAdmin(igrejaId) || podeEditarTexto(igrejaId, pregadorPessoaId) || podeEditarLouvor(igrejaId)
                || pertenceAMidia(igrejaId)) {
            return;
        }
        throw new AccessDeniedException("Sem permissão para ver o conteúdo deste culto");
    }

    public boolean podeEditarTexto(UUID igrejaId, UUID pregadorPessoaId) {
        Usuario usuario = requireUsuario();
        if (isPastorOuAdmin(igrejaId)) {
            return true;
        }
        return usuario.getPessoaId() != null && usuario.getPessoaId().equals(pregadorPessoaId);
    }

    public boolean podeEditarLouvor(UUID igrejaId) {
        if (isPastorOuAdmin(igrejaId)) {
            return true;
        }
        UUID pessoaId = requireUsuario().getPessoaId();
        if (pessoaId == null) {
            return false;
        }
        return ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId).stream()
                .anyMatch(m -> m.getTipo() == MinisterioTipo.MUSICA
                        && (pessoaId.equals(m.getLiderPessoaId()) || pessoaId.equals(m.getCoordenadorPessoaId())));
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
        if (isPastorOuAdmin(pessoa.getIgrejaId())) {
            return;
        }
        if (usuario.getPessoaId() == null || !usuario.getPessoaId().equals(pessoa.getId())) {
            throw new AccessDeniedException("Só é possível gerir a própria disponibilidade");
        }
    }

    public void assertPodeCriarUsuario(UUID igrejaId) {
        assertPastorOuAdmin(igrejaId);
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
                        "Não é permitido atribuir ADMIN/PASTOR neste atalho. Use o endpoint de promoção."
                );
            }
            if (!PAPEIS_ATALHO.contains(papel)) {
                throw new AccessDeniedException("Papel não permitido neste atalho: " + papel);
            }
            normalizados.add(papel);
        }
        return normalizados;
    }

    public void assertPodePromoverGestao(UUID igrejaAlvoId) {
        assertPastorOuAdmin(igrejaAlvoId);
        IgrejaContextoService ctx = igrejaContextoService;
        if (!ctx.isMatriz() && !ctx.igrejaAtualId().equals(igrejaAlvoId)) {
            throw new AccessDeniedException("Somente pastor/admin da igreja alvo ou da matriz podem promover gestão");
        }
    }

    public void assertPessoaSemUsuario(boolean jaTemUsuario) {
        if (jaTemUsuario) {
            throw new ConflictException("Esta pessoa já possui usuário vinculado");
        }
    }
}
