package com.example.base_api.config;

import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.entity.IgrejaTipo;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.jornada.service.JornadaService;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.entity.UsuarioPapel;
import com.example.base_api.usuario.repository.PapelRepository;
import com.example.base_api.usuario.repository.UsuarioPapelRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Cria o primeiro usuário administrador da matriz em um banco sem usuários (fora do perfil dev).
 * Ativado pelas variáveis ADMIN_USERNAME / ADMIN_EMAIL / ADMIN_SENHA (e opcionalmente ADMIN_NOME).
 * Só age quando a tabela de usuários está vazia; depois disso as variáveis podem ser removidas.
 */
@Component
@Profile("!dev & !test")
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    static final int SENHA_MIN = 8;
    static final List<String> PAPEIS_ADMIN = List.of("ADMIN", "PASTOR");

    private final Environment environment;
    private final IgrejaRepository igrejaRepository;
    private final PessoaRepository pessoaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioPapelRepository usuarioPapelRepository;
    private final PapelRepository papelRepository;
    private final JornadaService jornadaService;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(
            Environment environment,
            IgrejaRepository igrejaRepository,
            PessoaRepository pessoaRepository,
            UsuarioRepository usuarioRepository,
            UsuarioPapelRepository usuarioPapelRepository,
            PapelRepository papelRepository,
            JornadaService jornadaService,
            PasswordEncoder passwordEncoder
    ) {
        this.environment = environment;
        this.igrejaRepository = igrejaRepository;
        this.pessoaRepository = pessoaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioPapelRepository = usuarioPapelRepository;
        this.papelRepository = papelRepository;
        this.jornadaService = jornadaService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = valor("app.bootstrap.admin.username");
        String email = valor("app.bootstrap.admin.email");
        String senha = valor("app.bootstrap.admin.senha");
        String nome = valor("app.bootstrap.admin.nome");
        boolean configurado = username != null || email != null || senha != null;

        if (usuarioRepository.count() > 0) {
            if (configurado) {
                log.info("Bootstrap de admin ignorado: já existem usuários. As variáveis ADMIN_* podem ser removidas.");
            }
            return;
        }

        if (!configurado) {
            log.warn("Nenhum usuário cadastrado. Defina ADMIN_USERNAME, ADMIN_EMAIL e ADMIN_SENHA "
                    + "para criar o primeiro administrador na próxima inicialização.");
            return;
        }

        if (username == null || email == null || senha == null) {
            throw new IllegalStateException("Bootstrap de admin: ADMIN_USERNAME, ADMIN_EMAIL e ADMIN_SENHA são obrigatórios");
        }
        if (senha.length() < SENHA_MIN) {
            throw new IllegalStateException("Bootstrap de admin: ADMIN_SENHA deve ter pelo menos " + SENHA_MIN + " caracteres");
        }

        Igreja matriz = igrejaRepository.findAll().stream()
                .filter(i -> i.getTipo() == IgrejaTipo.MATRIZ)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Bootstrap de admin: nenhuma igreja MATRIZ encontrada"));

        String emailNormalizado = email.trim().toLowerCase();

        Pessoa pessoa = pessoaRepository.findByEmailIgnoreCase(emailNormalizado).orElseGet(() -> {
            Pessoa p = new Pessoa();
            p.setNome(nome != null ? nome : username);
            p.setEmail(emailNormalizado);
            p.setIgrejaId(matriz.getId());
            p.setDataCadastro(LocalDate.now());
            p.setAtivo(true);
            Pessoa salva = pessoaRepository.save(p);
            jornadaService.criarJornadaInicial(salva.getId());
            return salva;
        });

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setEmail(emailNormalizado);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setPessoaId(pessoa.getId());
        usuario.setIgrejaId(matriz.getId());
        usuario.setAtivo(true);
        Usuario salvo = usuarioRepository.save(usuario);

        for (String nomePapel : PAPEIS_ADMIN) {
            Papel papel = papelRepository.findByNomeAndIgrejaIdIsNull(nomePapel)
                    .orElseThrow(() -> new IllegalStateException("Papel " + nomePapel + " não encontrado"));
            UsuarioPapel vinculo = new UsuarioPapel();
            vinculo.setUsuarioId(salvo.getId());
            vinculo.setPapelId(papel.getId());
            vinculo.setIgrejaId(matriz.getId());
            vinculo.setPapel(papel);
            usuarioPapelRepository.save(vinculo);
        }

        log.info("Administrador inicial '{}' criado na igreja '{}'. Remova ADMIN_SENHA do ambiente.",
                username, matriz.getNome());
    }

    private String valor(String chave) {
        String v = environment.getProperty(chave);
        return v == null || v.isBlank() ? null : v.trim();
    }
}
