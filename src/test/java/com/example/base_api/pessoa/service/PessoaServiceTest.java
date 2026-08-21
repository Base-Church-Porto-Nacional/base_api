package com.example.base_api.pessoa.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.jornada.service.JornadaService;
import com.example.base_api.pessoa.dto.PessoaCreateRequest;
import com.example.base_api.pessoa.dto.PessoaResponse;
import com.example.base_api.pessoa.dto.PessoaUpdateRequest;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PessoaServiceTest {

    @Mock
    private PessoaRepository pessoaRepository;

    @Mock
    private JornadaService jornadaService;

    @InjectMocks
    private PessoaService pessoaService;

    private Pessoa pessoa;

    @BeforeEach
    void setUp() {
        pessoa = new Pessoa();
        pessoa.setId(UUID.randomUUID());
        pessoa.setNome("João da Silva");
        pessoa.setEmail("joao@email.com");
        pessoa.setAtivo(true);
        pessoa.setDataCadastro(LocalDate.now());
        pessoa.setCreatedAt(Instant.now());
        pessoa.setUpdatedAt(Instant.now());
    }

    @Test
    void deveCriarPessoaEIniciarJornada() {
        PessoaCreateRequest request = new PessoaCreateRequest(
                "Maria Souza", null, "maria@email.com", "11999999999", null, null
        );

        when(pessoaRepository.findByEmailIgnoreCase("maria@email.com")).thenReturn(Optional.empty());
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> {
            Pessoa salva = invocation.getArgument(0);
            salva.setId(UUID.randomUUID());
            salva.setCreatedAt(Instant.now());
            salva.setUpdatedAt(Instant.now());
            return salva;
        });

        PessoaResponse response = pessoaService.criar(request);

        assertThat(response.nome()).isEqualTo("Maria Souza");
        assertThat(response.email()).isEqualTo("maria@email.com");
        verify(jornadaService).criarJornadaInicial(response.id());
    }

    @Test
    void naoDeveCriarPessoaComEmailDuplicado() {
        when(pessoaRepository.findByEmailIgnoreCase("joao@email.com")).thenReturn(Optional.of(pessoa));

        PessoaCreateRequest request = new PessoaCreateRequest(
                "Outro", null, "joao@email.com", null, null, null
        );

        assertThatThrownBy(() -> pessoaService.criar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    void deveBuscarPessoaPorId() {
        when(pessoaRepository.findById(pessoa.getId())).thenReturn(Optional.of(pessoa));

        PessoaResponse response = pessoaService.buscarPorId(pessoa.getId());

        assertThat(response.id()).isEqualTo(pessoa.getId());
        assertThat(response.nome()).isEqualTo("João da Silva");
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

        PessoaUpdateRequest request = new PessoaUpdateRequest(
                "João Atualizado", null, "joao@email.com", "11888888888", null, true, "obs"
        );

        PessoaResponse response = pessoaService.atualizar(pessoa.getId(), request);

        assertThat(response.nome()).isEqualTo("João Atualizado");
        assertThat(response.observacoes()).isEqualTo("obs");
    }

    @Test
    void deveListarPessoas() {
        when(pessoaRepository.findAll()).thenReturn(List.of(pessoa));

        List<PessoaResponse> lista = pessoaService.listar();

        assertThat(lista).hasSize(1);
        ArgumentCaptor<Pessoa> unused = ArgumentCaptor.forClass(Pessoa.class);
        assertThat(unused.getAllValues()).isEmpty();
    }
}
