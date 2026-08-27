package com.example.base_api.voluntario.service;

import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.service.MinisterioService;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.service.ServicoService;
import com.example.base_api.voluntario.dto.PessoaServicoResponse;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoluntarioServiceTest {

    @Mock
    private PessoaServicoRepository pessoaServicoRepository;
    @Mock
    private PessoaService pessoaService;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private ServicoService servicoService;
    @Mock
    private MinisterioService ministerioService;
    @Mock
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private VoluntarioService voluntarioService;

    @Test
    void membroNaoListaVoluntariosDeOutraIgreja() {
        UUID igrejaVisivel = UUID.randomUUID();
        UUID pessoaVisivelId = UUID.randomUUID();
        UUID servicoId = UUID.randomUUID();
        UUID ministerioId = UUID.randomUUID();

        Pessoa pessoaVisivel = new Pessoa();
        pessoaVisivel.setId(pessoaVisivelId);
        pessoaVisivel.setNome("Membro");
        pessoaVisivel.setIgrejaId(igrejaVisivel);

        PessoaServico vinculo = new PessoaServico();
        vinculo.setId(UUID.randomUUID());
        vinculo.setPessoaId(pessoaVisivelId);
        vinculo.setServicoId(servicoId);
        vinculo.setAtivo(true);

        Servico servico = new Servico();
        servico.setId(servicoId);
        servico.setMinisterioId(ministerioId);
        servico.setNome("Violão");

        Ministerio ministerio = new Ministerio();
        ministerio.setId(ministerioId);
        ministerio.setNome("Louvor");
        ministerio.setIgrejaId(igrejaVisivel);

        when(autorizacaoService.idsParaListagem(null)).thenReturn(List.of(igrejaVisivel));
        when(pessoaRepository.findByIgrejaIdInOrderByNomeAsc(List.of(igrejaVisivel)))
                .thenReturn(List.of(pessoaVisivel));
        when(autorizacaoService.podeIncluirPessoaNaListagem(pessoaVisivel)).thenReturn(true);
        when(pessoaServicoRepository.findByPessoaIdIn(List.of(pessoaVisivelId))).thenReturn(List.of(vinculo));
        when(pessoaService.buscarEntidade(pessoaVisivelId)).thenReturn(pessoaVisivel);
        when(servicoService.buscarEntidade(servicoId)).thenReturn(servico);
        when(ministerioService.buscarEntidade(ministerioId)).thenReturn(ministerio);

        List<PessoaServicoResponse> lista = voluntarioService.listar();

        assertThat(lista).hasSize(1);
        assertThat(lista.getFirst().pessoaId()).isEqualTo(pessoaVisivelId);
        verify(pessoaServicoRepository, never()).findAll();
    }

    @Test
    void membroNaoLeVinculoDePessoaDeOutraIgreja() {
        UUID vinculoId = UUID.randomUUID();
        Pessoa pessoa = new Pessoa();
        pessoa.setId(UUID.randomUUID());
        pessoa.setIgrejaId(UUID.randomUUID());

        PessoaServico vinculo = new PessoaServico();
        vinculo.setId(vinculoId);
        vinculo.setPessoaId(pessoa.getId());

        when(pessoaServicoRepository.findById(vinculoId)).thenReturn(java.util.Optional.of(vinculo));
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        doThrow(new AccessDeniedException("Sem permissão para ver esta pessoa"))
                .when(autorizacaoService).assertPodeVerPessoa(pessoa);

        assertThatThrownBy(() -> voluntarioService.buscarPorId(vinculoId))
                .isInstanceOf(AccessDeniedException.class);
    }
}
