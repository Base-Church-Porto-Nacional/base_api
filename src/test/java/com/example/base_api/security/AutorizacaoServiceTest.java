package com.example.base_api.security;

import com.example.base_api.baseministerial.entity.BaseMinisterial;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.escala.entity.TipoEscala;
import com.example.base_api.escala.repository.EscalaRepository;
import com.example.base_api.escala.repository.EscalaVoluntarioRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.entity.IgrejaTipo;
import com.example.base_api.igreja.service.IgrejaContextoService;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.entity.MinisterioTipo;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.servico.repository.ServicoRepository;
import com.example.base_api.smallgroup.entity.SmallGroup;
import com.example.base_api.usuario.entity.Usuario;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorizacaoServiceTest {

    private static final Set<Capacidade> PASTOR = Capacidade.de(Set.of(Papeis.PASTOR), false);
    private static final Set<Capacidade> SECRETARIA = Capacidade.de(Set.of(Papeis.SECRETARIA), false);
    private static final Set<Capacidade> NENHUMA = Set.of();

    @Mock
    private IgrejaContextoService igrejaContextoService;
    @Mock
    private PermissaoService permissaoService;
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

    private void capacidades(Set<Capacidade> capacidades) {
        when(permissaoService.capacidadesEm(igrejaId)).thenReturn(capacidades);
    }

    private Igreja igreja(IgrejaTipo tipo) {
        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setTipo(tipo);
        return igreja;
    }

    // --- tem / assertTem ---

    @Test
    void assertTemVerificaAcessoAntesDaCapacidade() {
        doThrow(new AccessDeniedException("Sem acesso a esta igreja"))
                .when(igrejaContextoService).assertAcesso(igrejaId);

        assertThatThrownBy(() -> autorizacaoService.assertTem(igrejaId, Capacidade.EXPORTAR))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Sem acesso");
        verify(permissaoService, never()).capacidadesEm(any());
    }

    @Test
    void assertTemSemCapacidadeLanca403() {
        capacidades(NENHUMA);

        assertThatThrownBy(() -> autorizacaoService.assertTem(igrejaId, Capacidade.EXPORTAR))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Sem permissão para esta ação");
    }

    @Test
    void isGestaoEhVerTodasAsPessoas() {
        capacidades(SECRETARIA);
        assertThat(autorizacaoService.isGestao(igrejaId)).isTrue();
    }

    // --- escalas ---

    @Test
    void secretariaPodeCriarEscalaDePregador() {
        capacidades(SECRETARIA);

        assertThatCode(() -> autorizacaoService.assertPodeCriarEscala(
                TipoEscala.PREGADOR, igrejaId, "2026-08", null)).doesNotThrowAnyException();
        assertThatCode(() -> autorizacaoService.assertPodeCriarEscala(
                TipoEscala.SUPERVISOR, igrejaId, "2026-08", null)).doesNotThrowAnyException();
    }

    @Test
    void membroNaoCriaEscalaSupervisor() {
        capacidades(NENHUMA);

        assertThatThrownBy(() -> autorizacaoService.assertPodeCriarEscala(
                TipoEscala.SUPERVISOR, igrejaId, "2026-08", null
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("supervisor/pregador");
    }

    @Test
    void liderSoCriaEscalaDoProprioMinisterio() {
        UUID ministerioProprio = UUID.randomUUID();
        UUID ministerioAlheio = UUID.randomUUID();
        capacidades(NENHUMA);
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);
        when(escalaRepository.findByIgrejaIdAndCompetenciaAndTipo(igrejaId, "2026-08", TipoEscala.SUPERVISOR))
                .thenReturn(List.of());
        when(permissaoService.ministeriosLideradosEm(igrejaId)).thenReturn(Set.of(ministerioProprio));

        autorizacaoService.assertPodeCriarEscala(TipoEscala.MINISTERIO, igrejaId, "2026-08", ministerioProprio);

        assertThatThrownBy(() -> autorizacaoService.assertPodeCriarEscala(
                TipoEscala.MINISTERIO, igrejaId, "2026-08", ministerioAlheio
        )).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("próprio ministério");
    }

    // --- conteúdo do culto ---

    @Test
    void secretariaNaoEditaTextoDoCulto() {
        capacidades(SECRETARIA);
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);

        assertThat(autorizacaoService.podeEditarTexto(igrejaId, UUID.randomUUID())).isFalse();
    }

    @Test
    void pastorEditaTextoDoCulto() {
        capacidades(PASTOR);

        assertThat(autorizacaoService.podeEditarTexto(igrejaId, UUID.randomUUID())).isTrue();
    }

    @Test
    void pregadorEditaProprioTexto() {
        capacidades(NENHUMA);
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);

        assertThat(autorizacaoService.podeEditarTexto(igrejaId, pessoaId)).isTrue();
    }

    @Test
    void membroNaoEditaTextoDoCulto() {
        capacidades(NENHUMA);
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);

        assertThat(autorizacaoService.podeEditarTexto(igrejaId, UUID.randomUUID())).isFalse();
    }

    @Test
    void secretariaVeConteudoDoCulto() {
        capacidades(SECRETARIA);

        assertThatCode(() -> autorizacaoService.assertPodeVerConteudo(igrejaId, UUID.randomUUID()))
                .doesNotThrowAnyException();
    }

    @Test
    void liderDeMusicaEditaLouvorESecretariaNao() {
        Ministerio musica = new Ministerio();
        musica.setId(UUID.randomUUID());
        musica.setIgrejaId(igrejaId);
        musica.setTipo(MinisterioTipo.MUSICA);
        capacidades(NENHUMA);
        when(permissaoService.ministeriosLideradosEm(igrejaId)).thenReturn(Set.of(musica.getId()));
        when(ministerioRepository.findAllById(Set.of(musica.getId()))).thenReturn(List.of(musica));

        assertThat(autorizacaoService.podeEditarLouvor(igrejaId)).isTrue();
    }

    @Test
    void secretariaSemMinisterioDeMusicaNaoEditaLouvor() {
        capacidades(SECRETARIA);
        when(permissaoService.ministeriosLideradosEm(igrejaId)).thenReturn(Set.of());

        assertThat(autorizacaoService.podeEditarLouvor(igrejaId)).isFalse();
    }

    // --- papéis de gestão e filial ---

    @Test
    void secretariaNaoAtribuiPapeisDeGestao() {
        capacidades(SECRETARIA);

        assertThatThrownBy(() -> autorizacaoService.assertPodeAtribuirPapeisGestao(igrejaId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void pastorAtribuiPapeisDeGestao() {
        capacidades(PASTOR);

        assertThatCode(() -> autorizacaoService.assertPodeAtribuirPapeisGestao(igrejaId))
                .doesNotThrowAnyException();
        verify(igrejaContextoService).assertAcesso(igrejaId);
    }

    @Test
    void secretariaNaoCriaFilial() {
        when(igrejaContextoService.igrejaAtual()).thenReturn(igreja(IgrejaTipo.MATRIZ));
        capacidades(SECRETARIA);

        assertThatThrownBy(() -> autorizacaoService.assertPodeCriarFilial())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void pastorDaFilialNaoCriaFilial() {
        when(igrejaContextoService.igrejaAtual()).thenReturn(igreja(IgrejaTipo.FILIAL));

        assertThatThrownBy(() -> autorizacaoService.assertPodeCriarFilial())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void pastorDaMatrizCriaFilial() {
        when(igrejaContextoService.igrejaAtual()).thenReturn(igreja(IgrejaTipo.MATRIZ));
        capacidades(PASTOR);

        assertThatCode(() -> autorizacaoService.assertPodeCriarFilial()).doesNotThrowAnyException();
    }

    @Test
    void secretariaDaMatrizDisparaGeracaoDeEscalas() {
        when(igrejaContextoService.igrejaAtual()).thenReturn(igreja(IgrejaTipo.MATRIZ));
        capacidades(SECRETARIA);

        assertThatCode(() -> autorizacaoService.assertMatrizGestao()).doesNotThrowAnyException();
    }

    @Test
    void membroDaMatrizNaoDisparaGeracaoDeEscalas() {
        when(igrejaContextoService.igrejaAtual()).thenReturn(igreja(IgrejaTipo.MATRIZ));
        capacidades(NENHUMA);

        assertThatThrownBy(() -> autorizacaoService.assertMatrizGestao())
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- contagem ---

    @Test
    void contadorLancaContagem() {
        capacidades(Capacidade.de(Set.of(), true));

        assertThatCode(() -> autorizacaoService.assertPodeLancarContagem(igrejaId)).doesNotThrowAnyException();
    }

    @Test
    void semCapacidadeDeContagemNaoLanca() {
        capacidades(NENHUMA);

        assertThatThrownBy(() -> autorizacaoService.assertPodeLancarContagem(igrejaId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("contagem");
    }

    // --- ministérios, bases e grupos ---

    @Test
    void membroNaoCriaUsuario() {
        capacidades(NENHUMA);

        assertThatThrownBy(() -> autorizacaoService.assertPodeCriarUsuario(igrejaId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void secretariaCriaMinisterio() {
        capacidades(SECRETARIA);

        assertThatCode(() -> autorizacaoService.assertPodeCriarMinisterio(igrejaId)).doesNotThrowAnyException();
    }

    @Test
    void liderDeBaseGerenciaMinisterioDaBase() {
        Ministerio ministerio = new Ministerio();
        ministerio.setId(UUID.randomUUID());
        ministerio.setIgrejaId(igrejaId);
        capacidades(NENHUMA);
        when(permissaoService.ministeriosLideradosEm(igrejaId)).thenReturn(Set.of(ministerio.getId()));

        assertThatCode(() -> autorizacaoService.assertPodeGerenciarMinisterio(ministerio))
                .doesNotThrowAnyException();
    }

    @Test
    void membroNaoGerenciaMinisterioAlheio() {
        Ministerio ministerio = new Ministerio();
        ministerio.setId(UUID.randomUUID());
        ministerio.setIgrejaId(igrejaId);
        capacidades(NENHUMA);
        when(permissaoService.ministeriosLideradosEm(igrejaId)).thenReturn(Set.of());

        assertThatThrownBy(() -> autorizacaoService.assertPodeGerenciarMinisterio(ministerio))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void liderDaBaseGerenciaABase() {
        BaseMinisterial base = new BaseMinisterial();
        base.setId(UUID.randomUUID());
        base.setIgrejaId(igrejaId);
        capacidades(NENHUMA);
        when(permissaoService.basesLideradasEm(igrejaId)).thenReturn(Set.of(base.getId()));

        assertThatCode(() -> autorizacaoService.assertPodeGerenciarBaseMinisterial(base))
                .doesNotThrowAnyException();
    }

    @Test
    void liderDoMinisterioGerenciaGrupoDoMinisterio() {
        SmallGroup grupo = new SmallGroup();
        grupo.setId(UUID.randomUUID());
        grupo.setIgrejaId(igrejaId);
        grupo.setMinisterioId(UUID.randomUUID());
        capacidades(NENHUMA);
        when(permissaoService.gruposLideradosEm(igrejaId)).thenReturn(Set.of());
        when(permissaoService.ministeriosLideradosEm(igrejaId)).thenReturn(Set.of(grupo.getMinisterioId()));

        assertThatCode(() -> autorizacaoService.assertPodeGerenciarSmallGroup(grupo))
                .doesNotThrowAnyException();
    }

    @Test
    void membroNaoGerenciaGrupoAlheio() {
        SmallGroup grupo = new SmallGroup();
        grupo.setId(UUID.randomUUID());
        grupo.setIgrejaId(igrejaId);
        capacidades(NENHUMA);
        when(permissaoService.gruposLideradosEm(igrejaId)).thenReturn(Set.of());

        assertThatThrownBy(() -> autorizacaoService.assertPodeGerenciarSmallGroup(grupo))
                .isInstanceOf(AccessDeniedException.class);
    }

    // --- pessoas ---

    @Test
    void voluntarioNaoAlteraDisponibilidadeDeOutraPessoa() {
        Pessoa outra = pessoaDaIgreja();
        capacidades(NENHUMA);
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);

        assertThatThrownBy(() -> autorizacaoService.assertPodeAcessarDisponibilidade(outra))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("própria disponibilidade");
    }

    @Test
    void liderNaoVePessoaForaDoMinisterio() {
        Pessoa outra = pessoaDaIgreja();
        capacidades(NENHUMA);
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);
        when(permissaoService.ministeriosLideradosEm(igrejaId)).thenReturn(Set.of());

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
    void pastorVeTodasAsPessoasDaIgrejaConsultandoAsCapacidadesUmaVezPorIgreja() {
        List<Pessoa> pessoas = List.of(pessoaDaIgreja(), pessoaDaIgreja(), pessoaDaIgreja());
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);
        capacidades(PASTOR);

        assertThat(autorizacaoService.filtrarVisiveisNaListagem(pessoas)).containsExactlyElementsOf(pessoas);
        verify(permissaoService, times(1)).capacidadesEm(igrejaId);
        verify(permissaoService, never()).ministeriosLideradosEm(any());
    }

    @Test
    void liderVeASiMesmoEAsPessoasDosMinisteriosQueLidera() {
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
        Servico servicoMeu = new Servico();
        servicoMeu.setId(UUID.randomUUID());
        servicoMeu.setMinisterioId(meu.getId());
        PessoaServico vinculo = new PessoaServico();
        vinculo.setPessoaId(voluntario.getId());
        vinculo.setServicoId(servicoMeu.getId());
        vinculo.setAtivo(true);

        when(igrejaContextoService.requireUsuario()).thenReturn(membro);
        capacidades(NENHUMA);
        when(permissaoService.ministeriosLideradosEm(igrejaId)).thenReturn(Set.of(meu.getId()));
        when(ministerioRepository.findAllById(Set.of(meu.getId()))).thenReturn(List.of(meu));
        when(servicoRepository.findByMinisterioIdInOrderByNomeAsc(List.of(meu.getId()))).thenReturn(List.of(servicoMeu));
        when(pessoaServicoRepository.findByServicoIdInAndAtivoTrue(List.of(servicoMeu.getId()))).thenReturn(List.of(vinculo));

        List<Pessoa> visiveis = autorizacaoService.filtrarVisiveisNaListagem(List.of(
                eu, coordenador, voluntario, voluntarioInativoOuDeOutroMinisterio, liderDeOutroMinisterio));

        assertThat(visiveis).containsExactly(eu, coordenador, voluntario);
        verify(permissaoService, times(1)).ministeriosLideradosEm(igrejaId);
        verify(pessoaServicoRepository, times(1)).findByServicoIdInAndAtivoTrue(any());
    }

    @Test
    void membroSemMinisterioVeSomenteASiMesmo() {
        Pessoa eu = pessoaDaIgreja();
        eu.setId(pessoaId);
        Pessoa outra = pessoaDaIgreja();
        when(igrejaContextoService.requireUsuario()).thenReturn(membro);
        capacidades(NENHUMA);
        when(permissaoService.ministeriosLideradosEm(igrejaId)).thenReturn(Set.of());

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
