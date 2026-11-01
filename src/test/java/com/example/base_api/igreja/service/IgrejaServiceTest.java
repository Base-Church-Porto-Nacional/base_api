package com.example.base_api.igreja.service;

import com.example.base_api.igreja.dto.IgrejaFilialCreateRequest;
import com.example.base_api.igreja.dto.IgrejaResponse;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.entity.IgrejaTipo;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.PapelRepository;
import com.example.base_api.usuario.repository.UsuarioPapelRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IgrejaServiceTest {

    @Mock
    private IgrejaRepository igrejaRepository;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioPapelRepository usuarioPapelRepository;
    @Mock
    private PapelRepository papelRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private IgrejaContextoService igrejaContextoService;
    @Mock
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private IgrejaService igrejaService;

    @Test
    void deveListarIgrejasVisiveisDaMatriz() {
        Igreja matriz = igreja(IgrejaTipo.MATRIZ, null);
        Igreja filial = igreja(IgrejaTipo.FILIAL, matriz.getId());
        when(igrejaContextoService.igrejasVisiveis()).thenReturn(List.of(matriz, filial));

        List<IgrejaResponse> visiveis = igrejaService.listarVisiveis();

        assertThat(visiveis).hasSize(2);
        assertThat(visiveis.get(0).tipo()).isEqualTo(IgrejaTipo.MATRIZ);
        assertThat(visiveis.get(1).tipo()).isEqualTo(IgrejaTipo.FILIAL);
    }

    @Test
    void deveMontarResponseComPastorEAdmin() {
        UUID pastorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Igreja igreja = igreja(IgrejaTipo.MATRIZ, null);
        igreja.setPastorPessoaId(pastorId);
        igreja.setAdminUsuarioId(adminId);

        Pessoa pastor = new Pessoa();
        pastor.setId(pastorId);
        pastor.setNome("Ana Costa");
        Usuario admin = new Usuario();
        admin.setId(adminId);
        admin.setUsername("admin");
        admin.setPessoaId(pastorId);

        when(usuarioRepository.findAllById(List.of(adminId))).thenReturn(List.of(admin));
        when(pessoaRepository.nomesPorId(any())).thenReturn(Map.of(pastorId, pastor.getNome()));

        IgrejaResponse response = igrejaService.toResponse(igreja);

        assertThat(response.pastorNome()).isEqualTo("Ana Costa");
        assertThat(response.adminNome()).isEqualTo("Ana Costa");
    }

    @Test
    void membroNaoCriaFilial() {
        UUID matrizId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Somente pastor ou admin da matriz"))
                .when(autorizacaoService).assertMatrizPastorOuAdmin();

        IgrejaFilialCreateRequest request = new IgrejaFilialCreateRequest(
                "Nova Filial",
                UUID.randomUUID(),
                "Admin Filial",
                "admin.filial@base.church",
                "adminfilial",
                "admin123"
        );

        assertThatThrownBy(() -> igrejaService.criarFilial(matrizId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Igreja igreja(IgrejaTipo tipo, UUID matrizId) {
        Igreja igreja = new Igreja();
        igreja.setId(UUID.randomUUID());
        igreja.setNome(tipo == IgrejaTipo.MATRIZ ? "Base Church" : "Filial");
        igreja.setTipo(tipo);
        igreja.setIgrejaMatrizId(matrizId);
        igreja.setAtivo(true);
        igreja.setCreatedAt(Instant.now());
        igreja.setUpdatedAt(Instant.now());
        return igreja;
    }

    @Test
    void listarVisiveisBuscaPastoresEAdminsEmLote() {
        Pessoa pastora = new Pessoa();
        pastora.setId(UUID.randomUUID());
        pastora.setNome("Pra. Ana");
        Pessoa adminPessoa = new Pessoa();
        adminPessoa.setId(UUID.randomUUID());
        adminPessoa.setNome("Bruno");
        Usuario admin = new Usuario();
        admin.setId(UUID.randomUUID());
        admin.setUsername("bruno");
        admin.setPessoaId(adminPessoa.getId());
        Usuario adminSemPessoa = new Usuario();
        adminSemPessoa.setId(UUID.randomUUID());
        adminSemPessoa.setUsername("sistema");
        Igreja sede = igrejaDeTeste("Sede");
        sede.setPastorPessoaId(pastora.getId());
        sede.setAdminUsuarioId(admin.getId());
        Igreja filial = igrejaDeTeste("Filial");
        filial.setAdminUsuarioId(adminSemPessoa.getId());

        when(igrejaContextoService.igrejasVisiveis()).thenReturn(List.of(sede, filial));
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(admin, adminSemPessoa));
        when(pessoaRepository.nomesPorId(any()))
                .thenReturn(Map.of(pastora.getId(), "Pra. Ana", adminPessoa.getId(), "Bruno"));

        List<IgrejaResponse> igrejas = igrejaService.listarVisiveis();

        assertThat(igrejas).extracting(IgrejaResponse::pastorNome).containsExactly("Pra. Ana", null);
        assertThat(igrejas).extracting(IgrejaResponse::adminNome).containsExactly("Bruno", "sistema");
        verify(pessoaRepository, never()).findById(any());
        verify(usuarioRepository, never()).findById(any());
    }

    private Igreja igrejaDeTeste(String nome) {
        Igreja igreja = new Igreja();
        igreja.setId(UUID.randomUUID());
        igreja.setNome(nome);
        igreja.setTipo(IgrejaTipo.FILIAL);
        igreja.setAtivo(true);
        return igreja;
    }
}
