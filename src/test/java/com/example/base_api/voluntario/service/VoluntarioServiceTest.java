package com.example.base_api.voluntario.service;

import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.service.MinisterioService;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.service.ServicoService;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.voluntario.dto.PessoaServicoCreateRequest;
import com.example.base_api.voluntario.dto.PessoaServicoResponse;
import com.example.base_api.voluntario.dto.PessoaServicoUpdateRequest;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
        when(autorizacaoService.filtrarVisiveisNaListagem(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pessoaServicoRepository.findByPessoaIdIn(List.of(pessoaVisivelId))).thenReturn(List.of(vinculo));
        when(pessoaService.nomesPorId(any())).thenReturn(Map.of(pessoaVisivelId, "Visível"));
        when(servicoService.buscarPorIds(any())).thenReturn(Map.of(servicoId, servico));
        when(ministerioService.nomesPorId(any())).thenReturn(Map.of(ministerioId, "Louvor"));

        List<PessoaServicoResponse> lista = voluntarioService.listar();

        assertThat(lista).hasSize(1);
        assertThat(lista.getFirst().pessoaId()).isEqualTo(pessoaVisivelId);
        assertThat(lista.getFirst().pessoaNome()).isEqualTo("Visível");
        assertThat(lista.getFirst().servicoNome()).isEqualTo("Violão");
        assertThat(lista.getFirst().ministerioNome()).isEqualTo("Louvor");
        verify(pessoaServicoRepository, never()).findAll();
        verify(pessoaService, never()).buscarEntidade(any());
        verify(servicoService, never()).buscarEntidade(any());
        verify(ministerioService, never()).buscarEntidade(any());
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

    // --- serviço fictício ou inativo não aceita vínculo de voluntário ---

    private final UUID igrejaId = UUID.randomUUID();

    private Pessoa pessoaDaIgreja() {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(UUID.randomUUID());
        pessoa.setIgrejaId(igrejaId);
        return pessoa;
    }

    private Servico servico(boolean ficcao, boolean ativo) {
        Servico servico = new Servico();
        servico.setId(UUID.randomUUID());
        servico.setMinisterioId(UUID.randomUUID());
        servico.setFiccao(ficcao);
        servico.setAtivo(ativo);
        return servico;
    }

    private Ministerio ministerioDe(Servico servico) {
        Ministerio ministerio = new Ministerio();
        ministerio.setId(servico.getMinisterioId());
        ministerio.setIgrejaId(igrejaId);
        when(ministerioService.buscarEntidade(servico.getMinisterioId())).thenReturn(ministerio);
        return ministerio;
    }

    private PessoaServicoCreateRequest criar(Pessoa pessoa, Servico servico) {
        return new PessoaServicoCreateRequest(pessoa.getId(), servico.getId(), null, null, null, null);
    }

    @Test
    void naoVinculaVoluntarioAoServicoFicticioCoordenador() {
        Pessoa pessoa = pessoaDaIgreja();
        Servico servico = servico(true, true);
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        when(servicoService.buscarEntidade(servico.getId())).thenReturn(servico);
        Ministerio ministerio = ministerioDe(servico);

        assertThatThrownBy(() -> voluntarioService.vincular(criar(pessoa, servico)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O serviço Coordenador é definido pela coordenação do ministério, não por vínculo de voluntário.");
        verify(autorizacaoService).assertPodeGerenciarMinisterio(ministerio);
        verify(pessoaServicoRepository, never()).save(any());
    }

    @Test
    void naoVinculaVoluntarioAServicoInativo() {
        Pessoa pessoa = pessoaDaIgreja();
        Servico servico = servico(false, false);
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        when(servicoService.buscarEntidade(servico.getId())).thenReturn(servico);
        ministerioDe(servico);

        assertThatThrownBy(() -> voluntarioService.vincular(criar(pessoa, servico)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Este serviço está inativo.");
        verify(pessoaServicoRepository, never()).save(any());
    }

    @Test
    void semPermissaoNoMinisterioRecebeAcessoNegadoAntesDaRegraDoServico() {
        Pessoa pessoa = pessoaDaIgreja();
        Servico servico = servico(true, false);
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        when(servicoService.buscarEntidade(servico.getId())).thenReturn(servico);
        Ministerio ministerio = ministerioDe(servico);
        doThrow(new AccessDeniedException("Sem permissão")).when(autorizacaoService).assertPodeGerenciarMinisterio(ministerio);

        assertThatThrownBy(() -> voluntarioService.vincular(criar(pessoa, servico)))
                .isInstanceOf(AccessDeniedException.class);
        verify(pessoaServicoRepository, never()).save(any());
    }

    private PessoaServico vinculoExistente(Pessoa pessoa, Servico servicoAtual) {
        PessoaServico vinculo = new PessoaServico();
        vinculo.setId(UUID.randomUUID());
        vinculo.setPessoaId(pessoa.getId());
        vinculo.setServicoId(servicoAtual.getId());
        vinculo.setAtivo(true);
        when(pessoaServicoRepository.findById(vinculo.getId())).thenReturn(Optional.of(vinculo));
        when(servicoService.buscarEntidade(servicoAtual.getId())).thenReturn(servicoAtual);
        return vinculo;
    }

    private PessoaServicoUpdateRequest atualizarPara(Pessoa pessoa, Servico servico, boolean ativo) {
        return new PessoaServicoUpdateRequest(pessoa.getId(), servico.getId(), null, null, ativo, null, null);
    }

    @Test
    void naoTrocaVinculoParaServicoFicticio() {
        Pessoa pessoa = pessoaDaIgreja();
        Servico atual = servico(false, true);
        ministerioDe(atual);
        PessoaServico vinculo = vinculoExistente(pessoa, atual);
        Servico novo = servico(true, true);
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        when(servicoService.buscarEntidade(novo.getId())).thenReturn(novo);
        ministerioDe(novo);

        assertThatThrownBy(() -> voluntarioService.atualizar(vinculo.getId(), atualizarPara(pessoa, novo, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O serviço Coordenador é definido pela coordenação do ministério, não por vínculo de voluntário.");
        assertThat(vinculo.getServicoId()).isEqualTo(atual.getId());
        verify(pessoaServicoRepository, never()).save(any());
    }

    @Test
    void naoTrocaVinculoParaServicoInativo() {
        Pessoa pessoa = pessoaDaIgreja();
        Servico atual = servico(false, true);
        ministerioDe(atual);
        PessoaServico vinculo = vinculoExistente(pessoa, atual);
        Servico novo = servico(false, false);
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        when(servicoService.buscarEntidade(novo.getId())).thenReturn(novo);
        ministerioDe(novo);

        assertThatThrownBy(() -> voluntarioService.atualizar(vinculo.getId(), atualizarPara(pessoa, novo, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Este serviço está inativo.");
        verify(pessoaServicoRepository, never()).save(any());
    }

    @Test
    void atualizarVinculoSemTrocarServicoInativoContinuaPermitido() {
        // Ex.: desativar o vínculo de um serviço que já foi desativado.
        Pessoa pessoa = pessoaDaIgreja();
        Servico atual = servico(false, false);
        ministerioDe(atual);
        PessoaServico vinculo = vinculoExistente(pessoa, atual);
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        when(pessoaServicoRepository.save(vinculo)).thenReturn(vinculo);
        when(pessoaService.nomesPorId(any())).thenReturn(Map.of());
        when(servicoService.buscarPorIds(any())).thenReturn(Map.of(atual.getId(), atual));
        when(ministerioService.nomesPorId(any())).thenReturn(Map.of());

        voluntarioService.atualizar(vinculo.getId(), atualizarPara(pessoa, atual, false));

        assertThat(vinculo.getAtivo()).isFalse();
        verify(pessoaServicoRepository).save(vinculo);
    }

    private void stubResposta(Servico... servicos) {
        when(pessoaService.nomesPorId(any())).thenReturn(Map.of());
        java.util.Map<UUID, Servico> porId = new java.util.HashMap<>();
        for (Servico sv : servicos) {
            porId.put(sv.getId(), sv);
        }
        when(servicoService.buscarPorIds(any())).thenReturn(porId);
        when(ministerioService.nomesPorId(any())).thenReturn(Map.of());
    }

    @Test
    void vinculoDoCoordenadorNaoTrocaDePessoa() {
        Pessoa coordenador = pessoaDaIgreja();
        Servico ficticio = servico(true, true);
        ministerioDe(ficticio);
        PessoaServico vinculo = vinculoExistente(coordenador, ficticio);
        Pessoa outra = pessoaDaIgreja();
        when(pessoaService.buscarEntidade(outra.getId())).thenReturn(outra);

        assertThatThrownBy(() -> voluntarioService.atualizar(vinculo.getId(), atualizarPara(outra, ficticio, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O serviço Coordenador é definido pela coordenação do ministério, não por vínculo de voluntário.");
        assertThat(vinculo.getPessoaId()).isEqualTo(coordenador.getId());
        verify(pessoaServicoRepository, never()).save(any());
    }

    @Test
    void vinculoDoCoordenadorNaoTrocaParaServicoReal() {
        Pessoa coordenador = pessoaDaIgreja();
        Servico ficticio = servico(true, true);
        ministerioDe(ficticio);
        PessoaServico vinculo = vinculoExistente(coordenador, ficticio);
        Servico real = servico(false, true);
        when(pessoaService.buscarEntidade(coordenador.getId())).thenReturn(coordenador);
        when(servicoService.buscarEntidade(real.getId())).thenReturn(real);
        ministerioDe(real);

        assertThatThrownBy(() -> voluntarioService.atualizar(vinculo.getId(), atualizarPara(coordenador, real, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O serviço Coordenador é definido pela coordenação do ministério, não por vínculo de voluntário.");
        verify(pessoaServicoRepository, never()).save(any());
    }

    @Test
    void vinculoDoCoordenadorSemTrocaContinuaEditavel() {
        Pessoa coordenador = pessoaDaIgreja();
        Servico ficticio = servico(true, true);
        ministerioDe(ficticio);
        PessoaServico vinculo = vinculoExistente(coordenador, ficticio);
        when(pessoaService.buscarEntidade(coordenador.getId())).thenReturn(coordenador);
        when(pessoaServicoRepository.save(vinculo)).thenReturn(vinculo);
        stubResposta(ficticio);

        voluntarioService.atualizar(vinculo.getId(), atualizarPara(coordenador, ficticio, true));

        verify(pessoaServicoRepository).save(vinculo);
    }

    @Test
    void naoReativaVinculoDeServicoInativo() {
        Pessoa pessoa = pessoaDaIgreja();
        Servico inativo = servico(false, false);
        ministerioDe(inativo);
        PessoaServico vinculo = vinculoExistente(pessoa, inativo);
        vinculo.setAtivo(false);
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);

        assertThatThrownBy(() -> voluntarioService.atualizar(vinculo.getId(), atualizarPara(pessoa, inativo, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Este serviço está inativo.");
        assertThat(vinculo.getAtivo()).isFalse();
        verify(pessoaServicoRepository, never()).save(any());
    }

    @Test
    void vinculoJaAtivoDeServicoInativoSegueEditavel() {
        Pessoa pessoa = pessoaDaIgreja();
        Servico inativo = servico(false, false);
        ministerioDe(inativo);
        PessoaServico vinculo = vinculoExistente(pessoa, inativo);
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        when(pessoaServicoRepository.save(vinculo)).thenReturn(vinculo);
        stubResposta(inativo);

        voluntarioService.atualizar(vinculo.getId(), atualizarPara(pessoa, inativo, true));

        verify(pessoaServicoRepository).save(vinculo);
    }
}
