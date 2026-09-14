package com.example.base_api.pessoa.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.jornada.service.JornadaService;
import com.example.base_api.pessoa.dto.PessoaCreateRequest;
import com.example.base_api.pessoa.dto.PessoaResponse;
import com.example.base_api.pessoa.dto.PessoaUpdateRequest;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.entity.UsuarioPapel;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PessoaServiceTest {

    @Mock
    private PessoaRepository pessoaRepository;

    @Mock
    private JornadaService jornadaService;

    @Mock
    private AutorizacaoService autorizacaoService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PessoaService pessoaService;

    private Pessoa pessoa;
    private UUID igrejaId;

    @BeforeEach
    void setUp() {
        igrejaId = UUID.randomUUID();
        pessoa = new Pessoa();
        pessoa.setId(UUID.randomUUID());
        pessoa.setNome("João da Silva");
        pessoa.setEmail("joao@email.com");
        pessoa.setAtivo(true);
        pessoa.setIgrejaId(igrejaId);
        pessoa.setDataCadastro(LocalDate.now());
        pessoa.setCreatedAt(Instant.now());
        pessoa.setUpdatedAt(Instant.now());
    }

    @Test
    void deveCriarPessoaEIniciarJornada() {
        PessoaCreateRequest request = new PessoaCreateRequest(
                "Maria Souza", null, "maria@email.com", "11999999999", null, null, igrejaId
        );

        when(pessoaRepository.findByEmailIgnoreCase("maria@email.com")).thenReturn(Optional.empty());
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> {
            Pessoa salva = invocation.getArgument(0);
            salva.setId(UUID.randomUUID());
            salva.setCreatedAt(Instant.now());
            salva.setUpdatedAt(Instant.now());
            return salva;
        });
        when(usuarioRepository.findByPessoaId(any())).thenReturn(Optional.empty());

        PessoaResponse response = pessoaService.criar(request);

        assertThat(response.nome()).isEqualTo("Maria Souza");
        assertThat(response.email()).isEqualTo("maria@email.com");
        assertThat(response.igrejaId()).isEqualTo(igrejaId);
        verify(jornadaService).criarJornadaInicial(response.id());
        verify(autorizacaoService).assertPodeMutarPessoas(igrejaId);
    }

    @Test
    void naoDeveCriarPessoaComEmailDuplicado() {
        when(pessoaRepository.findByEmailIgnoreCase("joao@email.com")).thenReturn(Optional.of(pessoa));

        PessoaCreateRequest request = new PessoaCreateRequest(
                "Outro", null, "joao@email.com", null, null, null, igrejaId
        );

        assertThatThrownBy(() -> pessoaService.criar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    void deveBuscarPessoaPorId() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());

        PessoaResponse response = pessoaService.buscarPorId(pessoa.getId());

        assertThat(response.id()).isEqualTo(pessoa.getId());
        assertThat(response.nome()).isEqualTo("João da Silva");
        assertThat(response.igrejaId()).isEqualTo(igrejaId);
        verify(autorizacaoService).assertPodeVerPessoa(pessoa);
    }

    @Test
    void deveLancarExcecaoQuandoPessoaNaoExiste() {
        UUID id = UUID.randomUUID();
        when(pessoaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pessoaService.buscarPorId(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveAtualizarPessoa() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        when(pessoaRepository.existsByEmailIgnoreCaseAndIdNot("joao@email.com", pessoa.getId())).thenReturn(false);
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());

        PessoaUpdateRequest request = new PessoaUpdateRequest(
                "João Atualizado", null, "joao@email.com", "11888888888", null, true, "obs"
        );

        PessoaResponse response = pessoaService.atualizar(pessoa.getId(), request);

        assertThat(response.nome()).isEqualTo("João Atualizado");
        assertThat(response.observacoes()).isEqualTo("obs");
        verify(autorizacaoService).assertPodeMutarPessoas(igrejaId);
    }

    @Test
    void deveListarPessoasDaIgrejaVisivel() {
        when(autorizacaoService.idsParaListagem(null)).thenReturn(List.of(igrejaId));
        when(pessoaRepository.findByIgrejaIdInOrderByNomeAsc(List.of(igrejaId))).thenReturn(List.of(pessoa));
        when(autorizacaoService.podeIncluirPessoaNaListagem(pessoa)).thenReturn(true);
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());

        List<PessoaResponse> lista = pessoaService.listar(null);

        assertThat(lista).hasSize(1);
    }

    @Test
    void filialNaoListaPessoasDeOutraIgreja() {
        UUID outraIgreja = UUID.randomUUID();
        doThrow(new AccessDeniedException("Sem acesso a esta igreja"))
                .when(autorizacaoService).idsParaListagem(outraIgreja);

        assertThatThrownBy(() -> pessoaService.listar(outraIgreja))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deveIncluirPapeisDaPessoaNaResposta() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.of(usuarioComPapeis()));

        PessoaResponse response = pessoaService.buscarPorId(pessoa.getId());

        assertThat(response.papeis()).contains("VOLUNTARIO", "LIDER");
    }

    @Test
    void naoDeveIncluirPapeisDeIgrejaDiferente() {
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.of(usuarioComPapeis()));

        PessoaResponse response = pessoaService.toResponse(pessoa);

        assertThat(response.papeis()).doesNotContain("ADMIN");
    }

    private Usuario usuarioComPapeis() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setPessoaId(pessoa.getId());
        usuario.setIgrejaId(igrejaId);
        usuario.setUsername("pessoa");
        usuario.setEmail("pessoa@email.com");
        usuario.setSenhaHash("hash");

        Papel voluntario = new Papel();
        voluntario.setId(UUID.randomUUID());
        voluntario.setNome("VOLUNTARIO");

        Papel lider = new Papel();
        lider.setId(UUID.randomUUID());
        lider.setNome("LIDER");

        Papel admin = new Papel();
        admin.setId(UUID.randomUUID());
        admin.setNome("ADMIN");

        UUID outraIgreja = UUID.randomUUID();

        UsuarioPapel vp = new UsuarioPapel();
        vp.setUsuarioId(usuario.getId());
        vp.setPapelId(voluntario.getId());
        vp.setIgrejaId(igrejaId);
        vp.setPapel(voluntario);

        UsuarioPapel lp = new UsuarioPapel();
        lp.setUsuarioId(usuario.getId());
        lp.setPapelId(lider.getId());
        lp.setIgrejaId(igrejaId);
        lp.setPapel(lider);

        UsuarioPapel ap = new UsuarioPapel();
        ap.setUsuarioId(usuario.getId());
        ap.setPapelId(admin.getId());
        ap.setIgrejaId(outraIgreja);
        ap.setPapel(admin);

        usuario.getPapeisIgreja().add(vp);
        usuario.getPapeisIgreja().add(lp);
        usuario.getPapeisIgreja().add(ap);
        return usuario;
    }
}
