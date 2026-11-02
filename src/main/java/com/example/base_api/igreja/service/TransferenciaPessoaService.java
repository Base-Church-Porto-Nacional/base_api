package com.example.base_api.igreja.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.baseministerial.repository.BaseMinisterialRepository;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.security.Papeis;
import com.example.base_api.smallgroup.repository.SmallGroupRepository;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import com.example.base_api.usuario.service.UsuarioService;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Move uma pessoa (e sua conta, se houver) para outra igreja da rede. Cada pessoa pertence a uma igreja só:
 * a transferência remove os papéis da igreja antiga, invalida os tokens e desativa os vínculos de serviço.
 */
@Service
public class TransferenciaPessoaService {

    static final String MSG_PASTOR_TITULAR = "Esta pessoa é o pastor titular da igreja atual.";
    static final String MSG_LIDERANCA =
            "Esta pessoa lidera ministérios ou grupos na igreja atual. Troque a liderança antes de transferir.";

    private final AutorizacaoService autorizacaoService;
    private final IgrejaRepository igrejaRepository;
    private final PessoaRepository pessoaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final MinisterioRepository ministerioRepository;
    private final BaseMinisterialRepository baseMinisterialRepository;
    private final SmallGroupRepository smallGroupRepository;
    private final PessoaServicoRepository pessoaServicoRepository;
    private final AuditService auditService;

    public TransferenciaPessoaService(
            AutorizacaoService autorizacaoService,
            IgrejaRepository igrejaRepository,
            PessoaRepository pessoaRepository,
            UsuarioRepository usuarioRepository,
            UsuarioService usuarioService,
            MinisterioRepository ministerioRepository,
            BaseMinisterialRepository baseMinisterialRepository,
            SmallGroupRepository smallGroupRepository,
            PessoaServicoRepository pessoaServicoRepository,
            AuditService auditService
    ) {
        this.autorizacaoService = autorizacaoService;
        this.igrejaRepository = igrejaRepository;
        this.pessoaRepository = pessoaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.ministerioRepository = ministerioRepository;
        this.baseMinisterialRepository = baseMinisterialRepository;
        this.smallGroupRepository = smallGroupRepository;
        this.pessoaServicoRepository = pessoaServicoRepository;
        this.auditService = auditService;
    }

    /** Regras da transferência, sem alterar nada: permite validar antes de criar o destino. */
    @Transactional(readOnly = true)
    public void validar(Pessoa pessoa) {
        UUID origemId = pessoa.getIgrejaId();
        autorizacaoService.assertAcesso(origemId);

        if (igrejaRepository.existsByPastorPessoaId(pessoa.getId())) {
            throw new ConflictException(MSG_PASTOR_TITULAR);
        }
        if (!ministerioRepository.findLideradosOuCoordenados(origemId, pessoa.getId()).isEmpty()
                || !baseMinisterialRepository.findLideradasOuCoordenadas(origemId, pessoa.getId()).isEmpty()
                || !smallGroupRepository.findByIgrejaIdAndLiderPessoaId(origemId, pessoa.getId()).isEmpty()) {
            throw new ConflictException(MSG_LIDERANCA);
        }

        usuarioRepository.findByPessoaId(pessoa.getId())
                .filter(usuario -> usuario.nomesPapeisSistema(origemId).contains(Papeis.PASTOR))
                // Nunca deixa a igreja de origem sem pastor ativo.
                .ifPresent(usuarioService::assertRestaPastorAtivoSemEle);
    }

    @Transactional
    public void transferir(Pessoa pessoa, UUID igrejaDestinoId) {
        UUID origemId = pessoa.getIgrejaId();
        if (igrejaDestinoId.equals(origemId)) {
            return;
        }
        validar(pessoa);

        pessoa.setIgrejaId(igrejaDestinoId);
        pessoaRepository.save(pessoa);

        Usuario usuario = usuarioRepository.findByPessoaId(pessoa.getId()).orElse(null);
        if (usuario != null) {
            usuarioService.substituirPapeis(usuario, origemId, Set.of());
            usuario.setIgrejaId(igrejaDestinoId);
            usuario.invalidarTokens();
            usuarioRepository.save(usuario);
        }

        List<PessoaServico> vinculos = pessoaServicoRepository.findAtivosDaPessoaNaIgreja(pessoa.getId(), origemId);
        if (!vinculos.isEmpty()) {
            vinculos.forEach(vinculo -> vinculo.setAtivo(false));
            pessoaServicoRepository.saveAll(vinculos);
        }

        auditService.registrar(
                "TRANSFERIR_PESSOA",
                "pessoa",
                "pessoaId=" + pessoa.getId() + " origem=" + origemId + " destino=" + igrejaDestinoId
        );
    }
}
