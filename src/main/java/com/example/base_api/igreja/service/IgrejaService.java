package com.example.base_api.igreja.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.igreja.dto.IgrejaFilialCreateRequest;
import com.example.base_api.igreja.dto.IgrejaResponse;
import com.example.base_api.igreja.dto.IgrejaUpdateRequest;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.entity.IgrejaTipo;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.jornada.service.JornadaService;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.security.Capacidade;
import com.example.base_api.security.Papeis;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.StatusUsuario;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.entity.UsuarioPapel;
import com.example.base_api.usuario.repository.PapelRepository;
import com.example.base_api.usuario.repository.UsuarioPapelRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class IgrejaService {

    private final IgrejaRepository igrejaRepository;
    private final PessoaRepository pessoaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioPapelRepository usuarioPapelRepository;
    private final PapelRepository papelRepository;
    private final PasswordEncoder passwordEncoder;
    private final IgrejaContextoService igrejaContextoService;
    private final AutorizacaoService autorizacaoService;
    private final JornadaService jornadaService;

    public IgrejaService(
            IgrejaRepository igrejaRepository,
            PessoaRepository pessoaRepository,
            UsuarioRepository usuarioRepository,
            UsuarioPapelRepository usuarioPapelRepository,
            PapelRepository papelRepository,
            PasswordEncoder passwordEncoder,
            IgrejaContextoService igrejaContextoService,
            AutorizacaoService autorizacaoService,
            JornadaService jornadaService
    ) {
        this.igrejaRepository = igrejaRepository;
        this.pessoaRepository = pessoaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioPapelRepository = usuarioPapelRepository;
        this.papelRepository = papelRepository;
        this.passwordEncoder = passwordEncoder;
        this.igrejaContextoService = igrejaContextoService;
        this.autorizacaoService = autorizacaoService;
        this.jornadaService = jornadaService;
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

    @Transactional
    public IgrejaResponse criarFilial(UUID matrizId, IgrejaFilialCreateRequest request) {
        autorizacaoService.assertPodeCriarFilial();
        Igreja matriz = buscarEntidade(matrizId);
        if (!matriz.isMatriz()) {
            throw new BusinessException("A igreja informada não é matriz");
        }
        autorizacaoService.assertAcesso(matrizId);

        Pessoa pastor = pessoaRepository.findById(request.pastorPessoaId())
                .orElseThrow(() -> new ResourceNotFoundException("Pastor não encontrado"));

        String adminEmail = request.adminEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(adminEmail)) {
            throw new BusinessException("E-mail já está em uso");
        }

        Igreja filial = new Igreja();
        filial.setNome(request.nome().trim());
        filial.setTipo(IgrejaTipo.FILIAL);
        filial.setIgrejaMatrizId(matriz.getId());
        filial.setPastorPessoaId(pastor.getId());
        filial.setAtivo(true);
        Igreja salva = igrejaRepository.save(filial);

        Papel adminRole = papelRepository.findByNomeAndIgrejaIdIsNull(Papeis.SECRETARIA)
                .orElseThrow(() -> new IllegalStateException("Papel SECRETARIA não encontrado"));

        String senha = request.adminSenha().trim();

        // Toda conta tem pessoa: o admin da filial é cadastrado como pessoa da própria filial.
        Pessoa pessoaAdmin = new Pessoa();
        pessoaAdmin.setNome(request.adminNome().trim());
        pessoaAdmin.setEmail(adminEmail);
        pessoaAdmin.setIgrejaId(salva.getId());
        pessoaAdmin.setDataCadastro(LocalDate.now());
        pessoaAdmin.setAtivo(true);
        Pessoa pessoaAdminSalva = pessoaRepository.save(pessoaAdmin);
        jornadaService.criarJornadaInicial(pessoaAdminSalva.getId());

        // Provisório até a Fase 3 (convite): conta já ATIVA, login pelo e-mail.
        Usuario admin = new Usuario();
        admin.setPessoaId(pessoaAdminSalva.getId());
        admin.setEmail(adminEmail);
        admin.setSenhaHash(passwordEncoder.encode(senha));
        admin.setStatus(StatusUsuario.ATIVO);
        admin.setIgrejaId(salva.getId());
        Usuario adminSalvo = usuarioRepository.save(admin);

        UsuarioPapel vinculo = new UsuarioPapel();
        vinculo.setUsuarioId(adminSalvo.getId());
        vinculo.setPapelId(adminRole.getId());
        vinculo.setIgrejaId(salva.getId());
        vinculo.setPapel(adminRole);
        usuarioPapelRepository.save(vinculo);

        return toResponse(igrejaRepository.save(salva));
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
