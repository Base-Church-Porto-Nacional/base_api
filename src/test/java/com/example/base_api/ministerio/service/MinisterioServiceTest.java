package com.example.base_api.ministerio.service;

import com.example.base_api.baseministerial.entity.BaseMinisterial;
import com.example.base_api.baseministerial.repository.BaseMinisterialRepository;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ResourceNotFoundException;
import com.example.base_api.ministerio.dto.MinisterioCreateRequest;
import com.example.base_api.ministerio.dto.MinisterioResponse;
import com.example.base_api.ministerio.dto.MinisterioUpdateRequest;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.entity.MinisterioTipo;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinisterioServiceTest {

    @Mock
    private MinisterioRepository ministerioRepository;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private PessoaServicoRepository pessoaServicoRepository;
    @Mock
    private BaseMinisterialRepository baseMinisterialRepository;

    @InjectMocks
    private MinisterioService ministerioService;

    @Test
    void deveCriarMinisterioComServicoCoordenador() {
        UUID igrejaId = UUID.randomUUID();
        when(ministerioRepository.findByIgrejaIdAndNomeIgnoreCase(igrejaId, "Louvor")).thenReturn(Optional.empty());
        when(ministerioRepository.save(any(Ministerio.class))).thenAnswer(invocation -> {
            Ministerio m = invocation.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            m.setUpdatedAt(Instant.now());
            return m;
        });
        when(servicoRepository.findByMinisterioIdAndNomeIgnoreCase(any(), eq("Coordenador")))
                .thenReturn(Optional.empty());
        when(servicoRepository.save(any(Servico.class))).thenAnswer(invocation -> {
            Servico s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setCreatedAt(Instant.now());
            s.setUpdatedAt(Instant.now());
            return s;
        });
        when(servicoRepository.findByMinisterioIdInOrderByNomeAsc(any())).thenReturn(List.of());

        MinisterioResponse response = ministerioService.criar(
                new MinisterioCreateRequest("Louvor", "Ministério de louvor", true, igrejaId, MinisterioTipo.MUSICA, null, null, null)
        );

        assertThat(response.nome()).isEqualTo("Louvor");
        assertThat(response.ativo()).isTrue();
        assertThat(response.tipo()).isEqualTo(MinisterioTipo.MUSICA);
        assertThat(response.igrejaId()).isEqualTo(igrejaId);
    }

    @Test
    void naoDeveCriarMinisterioDuplicadoNaMesmaIgreja() {
        UUID igrejaId = UUID.randomUUID();
        Ministerio existente = new Ministerio();
        existente.setId(UUID.randomUUID());
        existente.setNome("Louvor");
        when(ministerioRepository.findByIgrejaIdAndNomeIgnoreCase(igrejaId, "Louvor")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> ministerioService.criar(
                new MinisterioCreateRequest("Louvor", null, true, igrejaId, MinisterioTipo.GERAL, null, null, null)
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void naoDeveCriarMinisterioDuplicadoQuandoNomeVemComEspacos() {
        UUID igrejaId = UUID.randomUUID();
        Ministerio existente = new Ministerio();
        existente.setId(UUID.randomUUID());
        existente.setNome("Louvor");
        when(ministerioRepository.findByIgrejaIdAndNomeIgnoreCase(igrejaId, "Louvor")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> ministerioService.criar(
                new MinisterioCreateRequest("  Louvor ", null, true, igrejaId, MinisterioTipo.GERAL, null, null, null)
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um ministério com este nome");
    }

    @Test
    void naoDeveRenomearMinisterioParaNomeExistenteComEspacos() {
        UUID igrejaId = UUID.randomUUID();
        Ministerio ministerio = new Ministerio();
        ministerio.setId(UUID.randomUUID());
        ministerio.setIgrejaId(igrejaId);
        ministerio.setNome("Mídia");
        when(ministerioRepository.findById(ministerio.getId())).thenReturn(Optional.of(ministerio));
        when(ministerioRepository.existsByIgrejaIdAndNomeIgnoreCaseAndIdNot(igrejaId, "Louvor", ministerio.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> ministerioService.atualizar(
                ministerio.getId(),
                new MinisterioUpdateRequest(" Louvor  ", null, true, null, null, null, null, null, null)
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um ministério com este nome");
    }

    @Test
    void listarCarregaServicosVoluntariosENomesEmLote() {
        UUID igrejaId = UUID.randomUUID();
        Pessoa lider = pessoa("Lia");
        Pessoa coordenador = pessoa("Caio");
        BaseMinisterial base = new BaseMinisterial();
        base.setId(UUID.randomUUID());
        base.setNome("Artes");
        Ministerio louvor = ministerio("Louvor", igrejaId);
        louvor.setLiderPessoaId(lider.getId());
        louvor.setCoordenadorPessoaId(coordenador.getId());
        louvor.setBaseMinisterialId(base.getId());
        Ministerio midia = ministerio("Mídia", igrejaId);
        Servico vocal = servico(louvor.getId(), "Vocal", false);
        Servico coord = servico(louvor.getId(), "Coordenador", true);
        Servico camera = servico(midia.getId(), "Câmera", false);
        UUID pessoaA = UUID.randomUUID();
        UUID pessoaB = UUID.randomUUID();

        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(ministerioRepository.findByIgrejaIdInOrderByNomeAsc(List.of(igrejaId))).thenReturn(List.of(louvor, midia));
        when(servicoRepository.findByMinisterioIdInOrderByNomeAsc(List.of(louvor.getId(), midia.getId())))
                .thenReturn(List.of(vocal, coord, camera));
        when(pessoaServicoRepository.findByServicoIdInAndAtivoTrue(any())).thenReturn(List.of(
                vinculo(pessoaA, vocal), vinculo(pessoaB, vocal), vinculo(pessoaA, coord), vinculo(pessoaB, camera)));
        when(pessoaRepository.nomesPorId(any())).thenReturn(Map.of(lider.getId(), "Lia", coordenador.getId(), "Caio"));
        when(baseMinisterialRepository.findAllById(any())).thenReturn(List.of(base));

        List<MinisterioResponse> lista = ministerioService.listar(igrejaId);

        assertThat(lista).hasSize(2);
        MinisterioResponse r = lista.get(0);
        assertThat(r.liderNome()).isEqualTo("Lia");
        assertThat(r.coordenadorNome()).isEqualTo("Caio");
        assertThat(r.baseMinisterialNome()).isEqualTo("Artes");
        assertThat(r.totalVoluntarios()).isEqualTo(2);
        assertThat(r.totalServicos()).isEqualTo(2);
        assertThat(lista.get(1).totalVoluntarios()).isEqualTo(1);
        assertThat(lista.get(1).totalServicos()).isEqualTo(1);
        verify(pessoaServicoRepository, times(1)).findByServicoIdInAndAtivoTrue(any());
        verify(pessoaRepository, never()).findById(any());
        verify(servicoRepository, never()).findByMinisterioIdOrderByNomeAsc(any());
    }

    private Ministerio ministerio(String nome, UUID igrejaId) {
        Ministerio m = new Ministerio();
        m.setId(UUID.randomUUID());
        m.setNome(nome);
        m.setIgrejaId(igrejaId);
        m.setAtivo(true);
        return m;
    }

    private Servico servico(UUID ministerioId, String nome, boolean ficcao) {
        Servico s = new Servico();
        s.setId(UUID.randomUUID());
        s.setMinisterioId(ministerioId);
        s.setNome(nome);
        s.setFiccao(ficcao);
        s.setAtivo(true);
        return s;
    }

    private PessoaServico vinculo(UUID pessoaId, Servico servico) {
        PessoaServico v = new PessoaServico();
        v.setPessoaId(pessoaId);
        v.setServicoId(servico.getId());
        v.setAtivo(true);
        return v;
    }

    private Pessoa pessoa(String nome) {
        Pessoa p = new Pessoa();
        p.setId(UUID.randomUUID());
        p.setNome(nome);
        return p;
    }

    @Test
    void deveLancarQuandoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(ministerioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ministerioService.buscarPorId(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
