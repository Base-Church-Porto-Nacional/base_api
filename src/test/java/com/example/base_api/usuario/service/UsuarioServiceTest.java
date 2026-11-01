package com.example.base_api.usuario.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.security.JwtService;
import com.example.base_api.usuario.dto.UsuarioCreateRequest;
import com.example.base_api.usuario.dto.UsuarioResponse;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.entity.UsuarioPapel;
import com.example.base_api.usuario.repository.PapelRepository;
import com.example.base_api.usuario.repository.UsuarioPapelRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PapelRepository papelRepository;
    @Mock
    private UsuarioPapelRepository usuarioPapelRepository;
    @Mock
    private PessoaService pessoaService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private IgrejaRepository igrejaRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void deveCriarUsuarioComPapeis() {
        Papel membro = new Papel();
        membro.setId(UUID.randomUUID());
        membro.setNome("MEMBRO");
        UUID igrejaId = UUID.randomUUID();

        when(autorizacaoService.validarPapeisAtalho(Set.of("MEMBRO"))).thenReturn(Set.of("MEMBRO"));
        when(usuarioRepository.existsByUsername("joao")).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("joao@email.com")).thenReturn(false);
        when(papelRepository.findByNomeAndIgrejaIdIsNull("MEMBRO")).thenReturn(Optional.of(membro));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            u.setUpdatedAt(Instant.now());
            return u;
        });
        when(usuarioPapelRepository.save(any(UsuarioPapel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());

        UsuarioResponse response = usuarioService.criar(new UsuarioCreateRequest(
                null, "joao", "joao@email.com", "senha123", Set.of("MEMBRO"), igrejaId
        ));

        assertThat(response.username()).isEqualTo("joao");
        assertThat(response.papeis()).contains("MEMBRO");
        assertThat(response.igrejaId()).isEqualTo(igrejaId);
    }

    @Test
    void naoDeveCriarUsuarioComUsernameDuplicado() {
        UUID igrejaId = UUID.randomUUID();
        when(autorizacaoService.validarPapeisAtalho(Set.of("MEMBRO"))).thenReturn(Set.of("MEMBRO"));
        when(usuarioRepository.existsByUsername("joao")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criar(new UsuarioCreateRequest(
                null, "joao", "joao@email.com", "senha123", Set.of("MEMBRO"), igrejaId
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username");
    }

    @Test
    void naoDeveCriarUsuarioComPapelInvalido() {
        UUID igrejaId = UUID.randomUUID();
        when(autorizacaoService.validarPapeisAtalho(Set.of("XYZ"))).thenReturn(Set.of("XYZ"));
        when(usuarioRepository.existsByUsername("joao")).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("joao@email.com")).thenReturn(false);
        when(papelRepository.findByNomeAndIgrejaIdIsNull("XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.criar(new UsuarioCreateRequest(
                null, "joao", "joao@email.com", "senha123", Set.of("XYZ"), igrejaId
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Papel inválido");
    }

    @Test
    void membroNaoCriaUsuario() {
        UUID igrejaId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Somente pastor ou admin desta igreja"))
                .when(autorizacaoService).assertPodeCriarUsuario(igrejaId);

        assertThatThrownBy(() -> usuarioService.criar(new UsuarioCreateRequest(
                null, "joao", "joao@email.com", "senha123", Set.of("MEMBRO"), igrejaId
        ))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deveCriarUsuarioComPapelCustomizadoDaIgreja() {
        UUID igrejaId = UUID.randomUUID();
        Papel membro = new Papel();
        membro.setId(UUID.randomUUID());
        membro.setNome("MEMBRO");
        Papel contador = new Papel();
        contador.setId(UUID.randomUUID());
        contador.setNome("CONTADOR");
        contador.setIgrejaId(igrejaId);
        contador.setPodeLancarContagem(true);

        when(papelRepository.existsByNomeAndIgrejaId("CONTADOR", igrejaId)).thenReturn(true);
        when(papelRepository.existsByNomeAndIgrejaId("MEMBRO", igrejaId)).thenReturn(false);
        when(autorizacaoService.validarPapeisAtalho(Set.of("MEMBRO"))).thenReturn(Set.of("MEMBRO"));
        when(usuarioRepository.existsByUsername("ana")).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("ana@email.com")).thenReturn(false);
        when(papelRepository.findByNomeAndIgrejaIdIsNull("MEMBRO")).thenReturn(Optional.of(membro));
        when(papelRepository.findByNomeAndIgrejaIdIsNull("CONTADOR")).thenReturn(Optional.empty());
        when(papelRepository.findByNomeAndIgrejaId("CONTADOR", igrejaId)).thenReturn(Optional.of(contador));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(usuarioPapelRepository.save(any(UsuarioPapel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.empty());

        UsuarioResponse response = usuarioService.criar(new UsuarioCreateRequest(
                null, "ana", "ana@email.com", "senha123", Set.of("membro", "contador"), igrejaId
        ));

        assertThat(response.papeis()).containsExactlyInAnyOrder("MEMBRO", "CONTADOR");
    }

    @Test
    void papelCustomizadoDeOutraIgrejaNaoEhAceito() {
        UUID igrejaId = UUID.randomUUID();
        when(papelRepository.existsByNomeAndIgrejaId("CONTADOR", igrejaId)).thenReturn(false);
        when(autorizacaoService.validarPapeisAtalho(Set.of("CONTADOR")))
                .thenThrow(new AccessDeniedException("Papel não permitido neste atalho: CONTADOR"));

        assertThatThrownBy(() -> usuarioService.criar(new UsuarioCreateRequest(
                null, "ana", "ana@email.com", "senha123", Set.of("CONTADOR"), igrejaId
        ))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listarBuscaNomesDePessoasEIgrejasEmLote() {
        UUID igrejaId = UUID.randomUUID();
        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setNome("Sede");
        UUID pessoaA = UUID.randomUUID();
        Usuario comPessoa = new Usuario();
        comPessoa.setId(UUID.randomUUID());
        comPessoa.setUsername("ana");
        comPessoa.setPessoaId(pessoaA);
        comPessoa.setIgrejaId(igrejaId);
        Usuario semPessoa = new Usuario();
        semPessoa.setId(UUID.randomUUID());
        semPessoa.setUsername("sistema");
        semPessoa.setIgrejaId(igrejaId);

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(autorizacaoService.isPastorOuAdmin(igrejaId)).thenReturn(true);
        when(usuarioRepository.findByIgrejaIdIn(List.of(igrejaId))).thenReturn(List.of(comPessoa, semPessoa));
        when(pessoaService.nomesPorId(any())).thenReturn(Map.of(pessoaA, "Ana"));
        when(igrejaRepository.findAllById(any())).thenReturn(List.of(igreja));

        List<UsuarioResponse> lista = usuarioService.listar(igrejaId);

        assertThat(lista).extracting(UsuarioResponse::pessoaNome).containsExactly("Ana", null);
        assertThat(lista).extracting(UsuarioResponse::igrejaNome).containsExactly("Sede", "Sede");
        verify(pessoaService, never()).buscarEntidade(any());
        verify(igrejaRepository, never()).findById(any());
    }
}
