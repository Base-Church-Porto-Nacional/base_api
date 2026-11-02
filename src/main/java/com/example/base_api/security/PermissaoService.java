package com.example.base_api.security;

import com.example.base_api.baseministerial.repository.BaseMinisterialRepository;
import com.example.base_api.igreja.service.IgrejaContextoService;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.smallgroup.repository.SmallGroupRepository;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Única fonte de permissões no servidor: capacidades por papel e escopos derivados por vínculo. */
@Service
public class PermissaoService {

    private final IgrejaContextoService contexto;
    private final MinisterioRepository ministerioRepository;
    private final BaseMinisterialRepository baseMinisterialRepository;
    private final SmallGroupRepository smallGroupRepository;
    private final PessoaServicoRepository pessoaServicoRepository;

    public PermissaoService(IgrejaContextoService contexto,
                            MinisterioRepository ministerioRepository,
                            BaseMinisterialRepository baseMinisterialRepository,
                            SmallGroupRepository smallGroupRepository,
                            PessoaServicoRepository pessoaServicoRepository) {
        this.contexto = contexto;
        this.ministerioRepository = ministerioRepository;
        this.baseMinisterialRepository = baseMinisterialRepository;
        this.smallGroupRepository = smallGroupRepository;
        this.pessoaServicoRepository = pessoaServicoRepository;
    }

    public Permissoes doUsuarioAtual() {
        return em(contexto.requireUsuario().getIgrejaId());
    }

    public Set<Capacidade> capacidadesEm(UUID igrejaId) {
        return em(igrejaId).capacidades();
    }

    public Set<UUID> ministeriosLideradosEm(UUID igrejaId) {
        return em(igrejaId).ministeriosLiderados();
    }

    private Permissoes em(UUID igrejaId) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return calcular(igrejaId);
        }
        String chave = "permissoes:" + igrejaId;
        Object cacheado = attrs.getAttribute(chave, RequestAttributes.SCOPE_REQUEST);
        if (cacheado instanceof Permissoes p) {
            return p;
        }
        Permissoes calculado = calcular(igrejaId);
        attrs.setAttribute(chave, calculado, RequestAttributes.SCOPE_REQUEST);
        return calculado;
    }

    private Permissoes calcular(UUID igrejaId) {
        Usuario usuario = contexto.requireUsuario();
        // Capacidades vêm só de papéis de sistema (com cascata); papel customizado só dá contagem, na própria igreja.
        Set<Capacidade> capacidades = Capacidade.de(
                contexto.papeisSistemaNaIgreja(usuario, igrejaId),
                usuario.temPapelComContagem(igrejaId));
        Set<String> papeis = contexto.papeisNaIgreja(usuario, igrejaId);

        UUID pessoaId = usuario.getPessoaId();
        Set<UUID> ministerios = new HashSet<>();
        Set<UUID> bases = new HashSet<>();
        Set<UUID> grupos = new HashSet<>();
        boolean voluntario = false;
        if (pessoaId != null && igrejaId != null) {
            ministerioRepository.findLideradosOuCoordenados(igrejaId, pessoaId)
                    .forEach(m -> ministerios.add(m.getId()));
            baseMinisterialRepository.findLideradasOuCoordenadas(igrejaId, pessoaId)
                    .forEach(b -> bases.add(b.getId()));
            if (!bases.isEmpty()) {
                ministerioRepository.findByBaseMinisterialIdIn(bases)
                        .forEach(m -> ministerios.add(m.getId()));
            }
            smallGroupRepository.findByIgrejaIdAndLiderPessoaId(igrejaId, pessoaId)
                    .forEach(g -> grupos.add(g.getId()));
            voluntario = pessoaServicoRepository.existsByPessoaIdAndAtivoTrue(pessoaId);
        }
        return new Permissoes(igrejaId, papeis, capacidades, ministerios, bases, grupos, voluntario);
    }
}
