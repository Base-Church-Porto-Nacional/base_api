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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock private IgrejaRepository igrejaRepository;
    @Mock private PessoaRepository pessoaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioPapelRepository usuarioPapelRepository;
    @Mock private PapelRepository papelRepository;
    @Mock private JornadaService jornadaService;
    @Mock private PasswordEncoder passwordEncoder;

    private MockEnvironment environment;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
    }

    private AdminBootstrap bootstrap() {
        return new AdminBootstrap(environment, igrejaRepository, pessoaRepository, usuarioRepository,
                usuarioPapelRepository, papelRepository, jornadaService, passwordEncoder);
    }

    private void configurar(String username, String email, String senha) {
        environment.setProperty("app.bootstrap.admin.username", username);
        environment.setProperty("app.bootstrap.admin.email", email);
        environment.setProperty("app.bootstrap.admin.senha", senha);
    }

    @Test
    void naoFazNadaQuandoJaExistemUsuarios() {
        configurar("admin", "admin@igreja.com", "senhaForte123");
        when(usuarioRepository.count()).thenReturn(3L);

        bootstrap().run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoFazNadaQuandoNaoConfigurado() {
        when(usuarioRepository.count()).thenReturn(0L);

        bootstrap().run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void falhaQuandoConfiguracaoIncompleta() {
        environment.setProperty("app.bootstrap.admin.username", "admin");
        when(usuarioRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> bootstrap().run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("obrigatórios");
    }

    @Test
    void falhaQuandoSenhaCurta() {
        configurar("admin", "admin@igreja.com", "123");
        when(usuarioRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> bootstrap().run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_SENHA");
    }

    @Test
    void criaAdminEPastorNaMatrizQuandoBancoVazio() {
        configurar("admin", "Admin@Igreja.com", "senhaForte123");
        when(usuarioRepository.count()).thenReturn(0L);

        Igreja matriz = new Igreja();
        matriz.setId(UUID.randomUUID());
        matriz.setNome("Base Church");
        matriz.setTipo(IgrejaTipo.MATRIZ);
        when(igrejaRepository.findAll()).thenReturn(List.of(matriz));
        when(pessoaRepository.findByEmailIgnoreCase("admin@igreja.com")).thenReturn(Optional.empty());
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(inv -> {
            Pessoa p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(passwordEncoder.encode("senhaForte123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(papelRepository.findByNomeAndIgrejaIdIsNull(any())).thenAnswer(inv -> {
            Papel papel = new Papel();
            papel.setId(UUID.randomUUID());
            papel.setNome(inv.getArgument(0));
            return Optional.of(papel);
        });

        bootstrap().run(null);

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuario.capture());
        assertThat(usuario.getValue().getUsername()).isEqualTo("admin");
        assertThat(usuario.getValue().getEmail()).isEqualTo("admin@igreja.com");
        assertThat(usuario.getValue().getSenhaHash()).isEqualTo("hash");
        assertThat(usuario.getValue().getIgrejaId()).isEqualTo(matriz.getId());
        assertThat(usuario.getValue().getPessoaId()).isNotNull();
        verify(jornadaService).criarJornadaInicial(any());

        ArgumentCaptor<UsuarioPapel> vinculos = ArgumentCaptor.forClass(UsuarioPapel.class);
        verify(usuarioPapelRepository, times(2)).save(vinculos.capture());
        assertThat(vinculos.getAllValues()).extracting(v -> v.getPapel().getNome())
                .containsExactlyInAnyOrder("ADMIN", "PASTOR");
    }
}
