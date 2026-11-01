package com.example.base_api.igreja.service;

import com.example.base_api.common.exception.UnauthorizedException;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.entity.IgrejaTipo;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.security.UsuarioAutenticado;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.entity.UsuarioPapel;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
class IgrejaContextoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private IgrejaRepository igrejaRepository;

    @InjectMocks
    private IgrejaContextoService igrejaContextoService;

    private Usuario usuario;
    private Igreja filial;
    private UUID outraIgrejaId;

    @BeforeEach
    void setUp() {
        UUID igrejaId = UUID.randomUUID();
        outraIgrejaId = UUID.randomUUID();

        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setUsername("membro");
        usuario.setIgrejaId(igrejaId);

        filial = new Igreja();
        filial.setId(igrejaId);
        filial.setNome("Filial");
        filial.setTipo(IgrejaTipo.FILIAL);
        filial.setAtivo(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void semAutenticacaoLanca401() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> igrejaContextoService.requireUsuario())
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> igrejaContextoService.assertAcesso(filial.getId()))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> igrejaContextoService.idsParaListagem(null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void assertAcessoNullLanca403() {
        autenticar();

        assertThatThrownBy(() -> igrejaContextoService.assertAcesso(null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Igreja obrigatória");
    }

    @Test
    void filialNaoAcessaOutraIgreja() {
        autenticar();

        assertThatThrownBy(() -> igrejaContextoService.assertAcesso(outraIgrejaId))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> igrejaContextoService.idsParaListagem(outraIgrejaId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void filialListaSomenteAPropriaIgreja() {
        autenticar();

        List<UUID> ids = igrejaContextoService.idsParaListagem(null);

        assertThat(ids).containsExactly(filial.getId());
    }

    @Test
    void principalComIdBuscaUsuarioPorIdSemConsultarPorUsername() {
        autenticarComId();

        assertThat(igrejaContextoService.requireUsuario()).isSameAs(usuario);
        verify(usuarioRepository, never()).findByUsername(any());
    }

    @Test
    void assertPastorOuAdminResolveOUsuarioUmaUnicaVez() {
        usuario.getPapeisIgreja().add(vinculo("PASTOR", filial.getId()));
        autenticarComId();

        igrejaContextoService.assertPastorOuAdmin(filial.getId());

        verify(usuarioRepository, times(1)).findById(usuario.getId());
    }

    @Test
    void assertPastorOuAdminNegaMembroComum() {
        usuario.getPapeisIgreja().add(vinculo("MEMBRO", filial.getId()));
        autenticarComId();

        assertThatThrownBy(() -> igrejaContextoService.assertPastorOuAdmin(filial.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Somente pastor ou admin");
    }

    @Test
    void pastorDaMatrizHerdaPermissaoNaFilial() {
        Igreja matriz = new Igreja();
        matriz.setId(UUID.randomUUID());
        matriz.setTipo(IgrejaTipo.MATRIZ);
        matriz.setAtivo(true);
        filial.setIgrejaMatrizId(matriz.getId());
        usuario.setIgrejaId(matriz.getId());
        usuario.getPapeisIgreja().add(vinculo("PASTOR", matriz.getId()));
        autenticarComId();
        when(igrejaRepository.findById(matriz.getId())).thenReturn(Optional.of(matriz));
        when(igrejaRepository.findByIgrejaMatrizIdOrderByNomeAsc(matriz.getId())).thenReturn(List.of(filial));

        igrejaContextoService.assertPastorOuAdmin(filial.getId());

        assertThat(igrejaContextoService.idsParaListagem(null)).containsExactly(matriz.getId(), filial.getId());
    }

    private UsuarioPapel vinculo(String nomePapel, UUID igrejaId) {
        Papel papel = new Papel();
        papel.setId(UUID.randomUUID());
        papel.setNome(nomePapel);
        UsuarioPapel vinculo = new UsuarioPapel();
        vinculo.setUsuarioId(usuario.getId());
        vinculo.setPapelId(papel.getId());
        vinculo.setIgrejaId(igrejaId);
        vinculo.setPapel(papel);
        return vinculo;
    }

    private void autenticarComId() {
        usuario.setSenhaHash("hash");
        usuario.setAtivo(true);
        UsuarioAutenticado principal = UsuarioAutenticado.de(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        org.mockito.Mockito.lenient().when(igrejaRepository.findById(filial.getId())).thenReturn(Optional.of(filial));
    }

    private void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("membro", "n/a", List.of())
        );
        when(usuarioRepository.findByUsername("membro")).thenReturn(Optional.of(usuario));
        org.mockito.Mockito.lenient().when(igrejaRepository.findById(filial.getId())).thenReturn(Optional.of(filial));
    }
}
