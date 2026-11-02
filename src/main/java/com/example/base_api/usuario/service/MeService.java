package com.example.base_api.usuario.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.igreja.dto.IgrejaResponse;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.entity.IgrejaTipo;
import com.example.base_api.igreja.service.IgrejaContextoService;
import com.example.base_api.igreja.service.IgrejaService;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.Identificador;
import com.example.base_api.security.PermissaoService;
import com.example.base_api.security.UsuarioAutenticado;
import com.example.base_api.usuario.dto.MeResponse;
import com.example.base_api.usuario.dto.MeUpdateRequest;
import com.example.base_api.usuario.dto.PermissoesResponse;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.example.base_api.common.util.Textos.blankToNull;

@Service
public class MeService {

    private final UsuarioRepository usuarioRepository;
    private final PessoaRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;
    private final IgrejaContextoService igrejaContextoService;
    private final IgrejaService igrejaService;
    private final PermissaoService permissaoService;

    public MeService(
            UsuarioRepository usuarioRepository,
            PessoaRepository pessoaRepository,
            PasswordEncoder passwordEncoder,
            IgrejaContextoService igrejaContextoService,
            IgrejaService igrejaService,
            PermissaoService permissaoService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.pessoaRepository = pessoaRepository;
        this.passwordEncoder = passwordEncoder;
        this.igrejaContextoService = igrejaContextoService;
        this.igrejaService = igrejaService;
        this.permissaoService = permissaoService;
    }

    @Transactional(readOnly = true)
    public MeResponse obterPerfil(Authentication authentication) {
        Usuario usuario = buscarUsuarioAutenticado(authentication);
        Pessoa pessoa = resolverPessoa(usuario);
        return toResponse(usuario, pessoa);
    }

    @Transactional
    public MeResponse atualizarPerfil(Authentication authentication, MeUpdateRequest request) {
        Usuario usuario = buscarUsuarioAutenticado(authentication);

        Pessoa pessoa = resolverPessoa(usuario);

        String emailNovo = Identificador.email(request.email()).orElse(null);
        if (emailNovo != null) {
            usuarioRepository.findByEmailIgnoreCase(emailNovo)
                    .filter(outro -> !outro.getId().equals(usuario.getId()))
                    .ifPresent(outro -> {
                        throw new ConflictException("E-mail já usado por outra conta");
                    });
        }

        String telefoneNovo = blankToNull(request.telefone());
        boolean telefoneMudou = pessoa != null && !Objects.equals(telefoneNovo, pessoa.getTelefone());
        String telefoneLogin = usuario.getTelefone();
        if (telefoneMudou) {
            String whatsapp = pessoa.getWhatsapp();
            telefoneLogin = Identificador.telefone(whatsapp).or(() -> Identificador.telefone(telefoneNovo)).orElse(null);
            if (telefoneLogin != null) {
                usuarioRepository.findByTelefone(telefoneLogin)
                        .filter(outro -> !outro.getId().equals(usuario.getId()))
                        .ifPresent(outro -> {
                            throw new ConflictException("Telefone já usado por outra conta");
                        });
            }
        }
        if (emailNovo == null && telefoneLogin == null) {
            throw new BusinessException("Informe e-mail ou telefone para entrar no app");
        }

        boolean trocaSenha = request.novaSenha() != null && !request.novaSenha().isBlank();
        if (trocaSenha && (request.senhaAtual() == null || request.senhaAtual().isBlank()
                || usuario.getSenhaHash() == null
                || !passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash()))) {
            throw new BusinessException("Senha atual incorreta");
        }

        usuario.setEmail(emailNovo);
        usuario.setTelefone(telefoneLogin);
        if (trocaSenha) {
            usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
            usuario.invalidarTokens();
        }
        usuarioRepository.save(usuario);

        if (pessoa != null) {
            pessoa.setNome(request.nome().trim());
            pessoa.setNomeSocial(blankToNull(request.nomeSocial()));
            pessoa.setEmail(emailNovo);
            pessoa.setTelefone(telefoneNovo);
            pessoa.setDataNascimento(request.dataNascimento());
            pessoa.setObservacoes(request.observacoes());
            pessoaRepository.save(pessoa);
        }

        return toResponse(usuario, pessoa);
    }

    private Usuario buscarUsuarioAutenticado(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException("Usuário não autenticado");
        }
        return UsuarioAutenticado.idDe(authentication).flatMap(usuarioRepository::findById)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"));
    }

    private Pessoa resolverPessoa(Usuario usuario) {
        if (usuario.getPessoaId() == null) {
            return null;
        }
        return pessoaRepository.findById(usuario.getPessoaId()).orElse(null);
    }

    private MeResponse toResponse(Usuario usuario, Pessoa pessoa) {
        Set<String> papeis = usuario.nomesPapeisDaIgrejaAtual();

        Igreja igreja = igrejaContextoService.igrejaAtual();
        List<IgrejaResponse> visiveis = igrejaContextoService.igrejasVisiveis().stream()
                .map(igrejaService::toResponse)
                .toList();

        IgrejaTipo tipo = igreja == null ? null : igreja.getTipo();
        String igrejaNome = igreja == null ? null : igreja.getNome();

        return new MeResponse(
                usuario.getId(),
                usuario.getId(),
                usuario.getEmail(),
                usuario.isAtivo(),
                papeis,
                pessoa != null ? pessoa.getId() : usuario.getPessoaId(),
                pessoa != null ? pessoa.getNome() : null,
                pessoa != null ? pessoa.getNomeSocial() : null,
                pessoa != null ? pessoa.getTelefone() : null,
                pessoa != null ? pessoa.getDataNascimento() : null,
                pessoa != null ? pessoa.getObservacoes() : null,
                usuario.getIgrejaId(),
                tipo,
                igrejaNome,
                visiveis,
                PermissoesResponse.de(permissaoService.doUsuarioAtual())
        );
    }
}
