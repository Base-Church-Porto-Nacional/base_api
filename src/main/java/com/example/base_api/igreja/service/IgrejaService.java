package com.example.base_api.igreja.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.igreja.dto.IgrejaFilialCreateRequest;
import com.example.base_api.igreja.dto.IgrejaResponse;
import com.example.base_api.igreja.dto.IgrejaUpdateRequest;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.entity.IgrejaTipo;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.entity.UsuarioPapel;
import com.example.base_api.usuario.repository.PapelRepository;
import com.example.base_api.usuario.repository.UsuarioPapelRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public IgrejaService(
            IgrejaRepository igrejaRepository,
            PessoaRepository pessoaRepository,
            UsuarioRepository usuarioRepository,
            UsuarioPapelRepository usuarioPapelRepository,
            PapelRepository papelRepository,
            PasswordEncoder passwordEncoder,
            IgrejaContextoService igrejaContextoService,
            AutorizacaoService autorizacaoService
    ) {
        this.igrejaRepository = igrejaRepository;
        this.pessoaRepository = pessoaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioPapelRepository = usuarioPapelRepository;
        this.papelRepository = papelRepository;
        this.passwordEncoder = passwordEncoder;
        this.igrejaContextoService = igrejaContextoService;
        this.autorizacaoService = autorizacaoService;
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
        autorizacaoService.assertMatrizPastorOuAdmin();
        Igreja matriz = buscarEntidade(matrizId);
        if (!matriz.isMatriz()) {
            throw new BusinessException("A igreja informada não é matriz");
        }
        autorizacaoService.assertAcesso(matrizId);

        Pessoa pastor = pessoaRepository.findById(request.pastorPessoaId())
                .orElseThrow(() -> new ResourceNotFoundException("Pastor não encontrado"));

        if (usuarioRepository.existsByUsername(request.adminUsername())) {
            throw new BusinessException("Username já está em uso");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(request.adminEmail())) {
            throw new BusinessException("E-mail já está em uso");
        }

        Igreja filial = new Igreja();
        filial.setNome(request.nome().trim());
        filial.setTipo(IgrejaTipo.FILIAL);
        filial.setIgrejaMatrizId(matriz.getId());
        filial.setPastorPessoaId(pastor.getId());
        filial.setAtivo(true);
        Igreja salva = igrejaRepository.save(filial);

        Papel adminRole = papelRepository.findByNomeAndIgrejaIdIsNull("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Papel ADMIN não encontrado"));

        String senha = request.adminSenha().trim();

        Usuario admin = new Usuario();
        admin.setUsername(request.adminUsername().trim());
        admin.setEmail(request.adminEmail().trim().toLowerCase());
        admin.setSenhaHash(passwordEncoder.encode(senha));
        admin.setAtivo(true);
        admin.setIgrejaId(salva.getId());
        Usuario adminSalvo = usuarioRepository.save(admin);

        UsuarioPapel vinculo = new UsuarioPapel();
        vinculo.setUsuarioId(adminSalvo.getId());
        vinculo.setPapelId(adminRole.getId());
        vinculo.setIgrejaId(salva.getId());
        vinculo.setPapel(adminRole);
        usuarioPapelRepository.save(vinculo);

        salva.setAdminUsuarioId(adminSalvo.getId());
        return toResponse(igrejaRepository.save(salva));
    }

    @Transactional
    public IgrejaResponse atualizar(UUID id, IgrejaUpdateRequest request) {
        autorizacaoService.assertPastorOuAdmin(id);
        Igreja igreja = buscarEntidade(id);

        Pessoa pastor = pessoaRepository.findById(request.pastorPessoaId())
                .orElseThrow(() -> new ResourceNotFoundException("Pastor não encontrado"));

        igreja.setNome(request.nome().trim());
        igreja.setPastorPessoaId(pastor.getId());
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

    /** Monta as respostas carregando pastores e administradores em lote. */
    public List<IgrejaResponse> toResponses(List<Igreja> igrejas) {
        List<UUID> adminIds = igrejas.stream()
                .map(Igreja::getAdminUsuarioId).filter(Objects::nonNull).distinct().toList();
        Map<UUID, Usuario> admins = adminIds.isEmpty()
                ? Map.of()
                : usuarioRepository.findAllById(adminIds).stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));
        Map<UUID, String> nomes = pessoaRepository.nomesPorId(Stream.concat(
                igrejas.stream().map(Igreja::getPastorPessoaId),
                admins.values().stream().map(Usuario::getPessoaId)).toList());

        return igrejas.stream().map(igreja -> {
            Usuario admin = igreja.getAdminUsuarioId() == null ? null : admins.get(igreja.getAdminUsuarioId());
            String adminNome = admin == null
                    ? null
                    : Objects.requireNonNullElse(
                            admin.getPessoaId() == null ? null : nomes.get(admin.getPessoaId()),
                            admin.getUsername());
            return new IgrejaResponse(
                    igreja.getId(),
                    igreja.getNome(),
                    igreja.getTipo(),
                    igreja.getIgrejaMatrizId(),
                    igreja.getPastorPessoaId(),
                    igreja.getPastorPessoaId() == null ? null : nomes.get(igreja.getPastorPessoaId()),
                    igreja.getAdminUsuarioId(),
                    adminNome,
                    igreja.getAtivo(),
                    igreja.getCreatedAt(),
                    igreja.getUpdatedAt()
            );
        }).toList();
    }
}
