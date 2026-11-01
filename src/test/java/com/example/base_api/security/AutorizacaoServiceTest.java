package com.example.base_api.security;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.igreja.service.IgrejaContextoService;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.entity.UsuarioPapel;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorizacaoServiceTest {

    @Mock
    private IgrejaContextoService igrejaContextoService;
    @Mock
    private MinisterioRepository ministerioRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private PessoaServicoRepository pessoaServicoRepository;
    @Mock
    private EscalaRepository escalaRepository;
    @Mock
    private EscalaVoluntarioRepository escalaVoluntarioRepository;

    @InjectMocks
    private AutorizacaoService autorizacaoService;

    private UUID igrejaId;
    private UUID pessoaId;
    private Usuario membro;

    @BeforeEach
    void setUp() {
        igrejaId = UUID.randomUUID();
        pessoaId = UUID.randomUUID();
        membro = new Usuario();
        membro.setId(UUID.randomUUID());
        membro.setPessoaId(pessoaId);
        membro.setIgrejaId(igrejaId);
    }

    @Test
    void membroNaoCriaFilial() {
        doThrow(new AccessDeniedException("Somente pastor ou admin da matriz"))
                .when(igrejaContextoService).assertMatrizPastorOuAdmin();

        assertThatThrownBy(() -> autorizacaoService.assertMatrizPastorOuAdmin())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void membroNaoCriaUsuario() {
        doThrow(new AccessDeniedException("Somente pastor ou admin desta igreja"))
                .when(igrejaContextoService).assertPastorOuAdmin(igrejaId);

        assertThatThrownBy(() -> autorizacaoService.assertPodeCriarUsuario(igrejaId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void atalhoRejeitaPapeisAdminPastor() {
        assertThatThrownBy(() -> autorizacaoService.validarPapeisAtalho(Set.of("ADMIN")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN/PASTOR");
        assertThatThrownBy(() -> autorizacaoService.validarPapeisAtalho(Set.of("PASTOR")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void membroNaoCriaEscalaSupervisor() {
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);
        when(igrejaContextoService.isPastorOuAdmin(igrejaId)).thenReturn(false);

        assertThatThrownBy(() -> autorizacaoService.assertPodeCriarEscala(
                TipoEscala.SUPERVISOR, igrejaId, "2026-08", null
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("supervisor/pregador");
    }

    @Test
    void membroNaoEditaTextoDoCulto() {
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);
        when(igrejaContextoService.isPastorOuAdmin(igrejaId)).thenReturn(false);

        boolean pode = autorizacaoService.podeEditarTexto(igrejaId, UUID.randomUUID());

        org.assertj.core.api.Assertions.assertThat(pode).isFalse();
    }

    @Test
    void voluntarioNaoAlteraDisponibilidadeDeOutraPessoa() {
        Pessoa outra = new Pessoa();
        outra.setId(UUID.randomUUID());
        outra.setIgrejaId(igrejaId);

        when(igrejaContextoService.requireUsuario()).thenReturn(membro);
        when(igrejaContextoService.isPastorOuAdmin(igrejaId)).thenReturn(false);

        assertThatThrownBy(() -> autorizacaoService.assertPodeAcessarDisponibilidade(outra))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("própria disponibilidade");
    }

    @Test
    void liderSoCriaEscalaDoProprioMinisterio() {
        UUID ministerioProprio = UUID.randomUUID();
        UUID ministerioAlheio = UUID.randomUUID();

        Ministerio proprio = new Ministerio();
        proprio.setId(ministerioProprio);
        proprio.setIgrejaId(igrejaId);
        proprio.setLiderPessoaId(pessoaId);

        Ministerio alheio = new Ministerio();
        alheio.setId(ministerioAlheio);
        alheio.setIgrejaId(igrejaId);
        alheio.setLiderPessoaId(UUID.randomUUID());

        when(igrejaContextoService.requireUsuario()).thenReturn(membro);
        when(igrejaContextoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(escalaRepository.findByIgrejaIdAndCompetenciaAndTipo(igrejaId, "2026-08", TipoEscala.SUPERVISOR))
                .thenReturn(java.util.List.of());
        when(ministerioRepository.findById(ministerioProprio)).thenReturn(Optional.of(proprio));
        when(ministerioRepository.findById(ministerioAlheio)).thenReturn(Optional.of(alheio));

        autorizacaoService.assertPodeCriarEscala(TipoEscala.MINISTERIO, igrejaId, "2026-08", ministerioProprio);

        assertThatThrownBy(() -> autorizacaoService.assertPodeCriarEscala(
                TipoEscala.MINISTERIO, igrejaId, "2026-08", ministerioAlheio
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("próprio ministério");
    }

    @Test
    void liderNaoVePessoaForaDoMinisterio() {
        Pessoa outra = new Pessoa();
        outra.setId(UUID.randomUUID());
        outra.setIgrejaId(igrejaId);

        when(igrejaContextoService.requireUsuario()).thenReturn(membro);
        when(igrejaContextoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId)).thenReturn(java.util.List.of());

        assertThatThrownBy(() -> autorizacaoService.assertPodeVerPessoa(outra))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void pessoaDeOutraIgrejaERejeitada() {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(UUID.randomUUID());
        pessoa.setIgrejaId(UUID.randomUUID());

        assertThatThrownBy(() -> autorizacaoService.assertPessoaDaIgreja(pessoa, igrejaId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pertence a esta igreja");
    }

    @Test
    void papelCustomizadoComPermissaoLancaContagem() {
        membro.getPapeisIgreja().add(vinculo(papelCustomizado("CONTADOR", true), igrejaId));
        when(igrejaContextoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);

        assertThat(autorizacaoService.podeLancarContagem(igrejaId)).isTrue();
    }

    @Test
    void papelCustomizadoSemPermissaoNaoLancaContagem() {
        membro.getPapeisIgreja().add(vinculo(papelCustomizado("RECEPCAO", false), igrejaId));
        when(igrejaContextoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);

        assertThatThrownBy(() -> autorizacaoService.assertPodeLancarContagem(igrejaId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("contagem");
    }

    @Test
    void permissaoDeContagemDeOutraIgrejaNaoVale() {
        membro.getPapeisIgreja().add(vinculo(papelCustomizado("CONTADOR", true), UUID.randomUUID()));
        when(igrejaContextoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);

        assertThat(autorizacaoService.podeLancarContagem(igrejaId)).isFalse();
    }

    @Test
    void pastorOuAdminLancaContagemSemPapelEspecial() {
        when(igrejaContextoService.isPastorOuAdmin(igrejaId)).thenReturn(true);

        assertThat(autorizacaoService.podeLancarContagem(igrejaId)).isTrue();
    }

    private Papel papelCustomizado(String nome, boolean podeLancarContagem) {
        Papel papel = new Papel();
        papel.setId(UUID.randomUUID());
        papel.setNome(nome);
        papel.setIgrejaId(igrejaId);
        papel.setPodeLancarContagem(podeLancarContagem);
        return papel;
    }

    private UsuarioPapel vinculo(Papel papel, UUID igrejaDoVinculo) {
        UsuarioPapel vinculo = new UsuarioPapel();
        vinculo.setUsuarioId(membro.getId());
        vinculo.setPapelId(papel.getId());
        vinculo.setIgrejaId(igrejaDoVinculo);
        vinculo.setPapel(papel);
        return vinculo;
    }

    @Test
    void pastorVeTodasAsPessoasDaIgrejaConsultandoOPapelUmaVezPorIgreja() {
        List<Pessoa> pessoas = List.of(pessoaDaIgreja(), pessoaDaIgreja(), pessoaDaIgreja());
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);
        when(igrejaContextoService.isPastorOuAdmin(igrejaId)).thenReturn(true);

        assertThat(autorizacaoService.filtrarVisiveisNaListagem(pessoas)).containsExactlyElementsOf(pessoas);
        verify(igrejaContextoService, times(1)).isPastorOuAdmin(igrejaId);
        verify(ministerioRepository, never()).findByIgrejaIdOrderByNomeAsc(any());
    }

    @Test
    void liderVeASiMesmoEAsPessoasDosSeusMinisteriosComPoucasConsultas() {
        Pessoa eu = pessoaDaIgreja();
        eu.setId(pessoaId);
        Pessoa coordenador = pessoaDaIgreja();
        Pessoa voluntario = pessoaDaIgreja();
        Pessoa voluntarioInativoOuDeOutroMinisterio = pessoaDaIgreja();
        Pessoa liderDeOutroMinisterio = pessoaDaIgreja();
        Ministerio meu = new Ministerio();
        meu.setId(UUID.randomUUID());
        meu.setIgrejaId(igrejaId);
        meu.setLiderPessoaId(pessoaId);
        meu.setCoordenadorPessoaId(coordenador.getId());
        Ministerio alheio = new Ministerio();
        alheio.setId(UUID.randomUUID());
        alheio.setIgrejaId(igrejaId);
        alheio.setLiderPessoaId(liderDeOutroMinisterio.getId());
        Servico servicoMeu = new Servico();
        servicoMeu.setId(UUID.randomUUID());
        servicoMeu.setMinisterioId(meu.getId());
        PessoaServico vinculo = new PessoaServico();
        vinculo.setPessoaId(voluntario.getId());
        vinculo.setServicoId(servicoMeu.getId());
        vinculo.setAtivo(true);

        when(igrejaContextoService.requireUsuario()).thenReturn(membro);
        when(igrejaContextoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId)).thenReturn(List.of(meu, alheio));
        when(servicoRepository.findByMinisterioIdInOrderByNomeAsc(List.of(meu.getId()))).thenReturn(List.of(servicoMeu));
        when(pessoaServicoRepository.findByServicoIdInAndAtivoTrue(List.of(servicoMeu.getId()))).thenReturn(List.of(vinculo));

        List<Pessoa> visiveis = autorizacaoService.filtrarVisiveisNaListagem(List.of(
                eu, coordenador, voluntario, voluntarioInativoOuDeOutroMinisterio, liderDeOutroMinisterio));

        assertThat(visiveis).containsExactly(eu, coordenador, voluntario);
        verify(ministerioRepository, times(1)).findByIgrejaIdOrderByNomeAsc(igrejaId);
        verify(pessoaServicoRepository, times(1)).findByServicoIdInAndAtivoTrue(any());
    }

    @Test
    void membroSemMinisterioVeSomenteASiMesmo() {
        Pessoa eu = pessoaDaIgreja();
        eu.setId(pessoaId);
        Pessoa outra = pessoaDaIgreja();
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);
        when(igrejaContextoService.isPastorOuAdmin(igrejaId)).thenReturn(false);
        when(ministerioRepository.findByIgrejaIdOrderByNomeAsc(igrejaId)).thenReturn(List.of());

        assertThat(autorizacaoService.filtrarVisiveisNaListagem(List.of(outra, eu))).containsExactly(eu);
        assertThat(autorizacaoService.podeIncluirPessoaNaListagem(outra)).isFalse();
        assertThat(autorizacaoService.podeIncluirPessoaNaListagem(eu)).isTrue();
    }

    private Pessoa pessoaDaIgreja() {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(UUID.randomUUID());
        pessoa.setIgrejaId(igrejaId);
        return pessoa;
    }
}
