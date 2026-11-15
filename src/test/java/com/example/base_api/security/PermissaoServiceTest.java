package com.example.base_api.security;

import com.example.base_api.baseministerial.entity.BaseMinisterial;
import com.example.base_api.baseministerial.repository.BaseMinisterialRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.igreja.service.IgrejaContextoService;
import com.example.base_api.ministerio.entity.Ministerio;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.smallgroup.entity.SmallGroup;
import com.example.base_api.smallgroup.repository.SmallGroupRepository;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.entity.UsuarioPapel;
import com.example.base_api.usuario.repository.UsuarioRepository;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissaoServiceTest {

    private final UUID matrizId = UUID.randomUUID();
    private final UUID filialId = UUID.randomUUID();
    private final UUID pessoaId = UUID.randomUUID();

    private IgrejaRepository igrejaRepository;
    private MinisterioRepository ministerioRepository;
    private BaseMinisterialRepository baseRepository;
    private SmallGroupRepository grupoRepository;
    private PessoaServicoRepository pessoaServicoRepository;
    private PermissaoService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        igrejaRepository = mock(IgrejaRepository.class);
        ministerioRepository = mock(MinisterioRepository.class);
        baseRepository = mock(BaseMinisterialRepository.class);
        grupoRepository = mock(SmallGroupRepository.class);
        pessoaServicoRepository = mock(PessoaServicoRepository.class);

        Igreja matriz = new Igreja();
        matriz.setId(matrizId);
        Igreja filial = new Igreja();
        filial.setId(filialId);
        filial.setIgrejaMatrizId(matrizId);
        when(igrejaRepository.findById(matrizId)).thenReturn(Optional.of(matriz));
        when(igrejaRepository.findById(filialId)).thenReturn(Optional.of(filial));

        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setIgrejaId(matrizId);
        usuario.setPessoaId(pessoaId);
        usuario.setPapeisIgreja(new HashSet<>());

        IgrejaContextoService contexto = spy(new IgrejaContextoService(mock(UsuarioRepository.class), igrejaRepository));
        doReturn(usuario).when(contexto).requireUsuario();
        service = new PermissaoService(contexto, ministerioRepository, baseRepository, grupoRepository,
                pessoaServicoRepository);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void dar(String nome, boolean sistema, boolean contagem, UUID igrejaId) {
        Papel papel = new Papel();
        papel.setId(UUID.randomUUID());
        papel.setNome(nome);
        papel.setIgrejaId(sistema ? null : igrejaId);
        papel.setPodeLancarContagem(contagem);
        UsuarioPapel up = new UsuarioPapel();
        up.setUsuarioId(usuario.getId());
        up.setPapelId(papel.getId());
        up.setIgrejaId(igrejaId);
        up.setPapel(papel);
        usuario.getPapeisIgreja().add(up);
    }

    @Test
    void pastorTemTodasAsCapacidades() {
        dar("PASTOR", true, false, matrizId);

        assertThat(service.doUsuarioAtual().capacidades()).containsExactlyInAnyOrder(Capacidade.values());
    }

    @Test
    void secretariaNaoTemExclusivasDoPastor() {
        dar("SECRETARIA", true, false, matrizId);

        Set<Capacidade> caps = service.capacidadesEm(matrizId);

        assertThat(caps).doesNotContain(Capacidade.CULTO_CONTEUDO_EDITAR, Capacidade.PAPEIS_GESTAO_ATRIBUIR,
                Capacidade.FILIAL_CRIAR);
        assertThat(caps).contains(Capacidade.ESCALAS_GERIR);
    }

    @Test
    void papelCustomComContagemDaCultoContagem() {
        dar("CONTADOR", false, true, matrizId);

        assertThat(service.capacidadesEm(matrizId)).containsExactly(Capacidade.CULTO_CONTAGEM);
    }

    @Test
    void papelCustomComNomeDeSistemaNaoConcedeNada() {
        dar("SECRETARIA", false, false, matrizId);

        assertThat(service.capacidadesEm(matrizId)).isEmpty();
    }

    @Test
    void papelCustomDaMatrizNaoDaContagemNaFilial() {
        dar("CONTADOR", false, true, matrizId);

        assertThat(service.capacidadesEm(filialId)).isEmpty();
    }

    @Test
    void semPapelLiderDeMinisterioTemEscopoMasNenhumaCapacidade() {
        UUID ministerioId = UUID.randomUUID();
        Ministerio m = new Ministerio();
        m.setId(ministerioId);
        when(ministerioRepository.findAtivosLideradosOuCoordenados(matrizId, pessoaId)).thenReturn(List.of(m));

        Permissoes p = service.doUsuarioAtual();

        assertThat(p.capacidades()).isEmpty();
        assertThat(p.ministeriosLiderados()).containsExactly(ministerioId);
    }

    @Test
    void liderDeBaseHerdaMinisteriosDaBase() {
        UUID baseId = UUID.randomUUID();
        UUID ministerioId = UUID.randomUUID();
        BaseMinisterial base = new BaseMinisterial();
        base.setId(baseId);
        Ministerio m = new Ministerio();
        m.setId(ministerioId);
        when(baseRepository.findAtivasLideradasOuCoordenadas(matrizId, pessoaId)).thenReturn(List.of(base));
        when(ministerioRepository.findByBaseMinisterialIdInAndAtivoTrue(any())).thenReturn(List.of(m));

        Permissoes p = service.doUsuarioAtual();

        assertThat(p.basesLideradas()).containsExactly(baseId);
        assertThat(service.ministeriosLideradosEm(matrizId)).containsExactly(ministerioId);
    }

    @Test
    void liderDeGrupoAparecemEmGruposLiderados() {
        UUID grupoId = UUID.randomUUID();
        SmallGroup g = new SmallGroup();
        g.setId(grupoId);
        when(grupoRepository.findByIgrejaIdAndLiderPessoaIdAndAtivoTrue(matrizId, pessoaId)).thenReturn(List.of(g));

        assertThat(service.doUsuarioAtual().gruposLiderados()).containsExactly(grupoId);
    }

    @Test
    void voluntarioAtivoEhVoluntario() {
        when(pessoaServicoRepository.existsByPessoaIdAndAtivoTrue(pessoaId)).thenReturn(true);

        assertThat(service.doUsuarioAtual().ehVoluntario()).isTrue();
    }

    @Test
    void vinculoInativoNaoEhVoluntario() {
        when(pessoaServicoRepository.existsByPessoaIdAndAtivoTrue(pessoaId)).thenReturn(false);

        assertThat(service.doUsuarioAtual().ehVoluntario()).isFalse();
    }

    @Test
    void usuarioSemPessoaNaoTemEscopos() {
        usuario.setPessoaId(null);

        Permissoes p = service.doUsuarioAtual();

        assertThat(p.ministeriosLiderados()).isEmpty();
        assertThat(p.ehVoluntario()).isFalse();
    }

    @Test
    void pastorDaMatrizTemCapacidadesNaFilial() {
        dar("PASTOR", true, false, matrizId);

        assertThat(service.capacidadesEm(filialId)).containsExactlyInAnyOrder(Capacidade.values());
    }

    @Test
    void funcionaSemRequisicaoHttp() {
        RequestContextHolder.resetRequestAttributes();
        dar("PASTOR", true, false, matrizId);

        assertThat(service.capacidadesEm(matrizId)).isNotEmpty();
    }

    @Test
    void cacheiaPorRequisicao() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        service.doUsuarioAtual();
        service.doUsuarioAtual();

        verify(ministerioRepository, times(1)).findAtivosLideradosOuCoordenados(matrizId, pessoaId);
    }

    // --- ministério, base ou grupo inativo não dá escopo de líder ---

    @Test
    void escopoDeLiderUsaSoConsultasDeEntidadesAtivas() {
        service.doUsuarioAtual();

        verify(ministerioRepository).findAtivosLideradosOuCoordenados(matrizId, pessoaId);
        verify(baseRepository).findAtivasLideradasOuCoordenadas(matrizId, pessoaId);
        verify(grupoRepository).findByIgrejaIdAndLiderPessoaIdAndAtivoTrue(matrizId, pessoaId);
        verify(ministerioRepository, never()).findLideradosOuCoordenados(any(), any());
        verify(baseRepository, never()).findLideradasOuCoordenadas(any(), any());
        verify(grupoRepository, never()).findByIgrejaIdAndLiderPessoaId(any(), any());
    }

    @Test
    void liderSoDeMinisterioInativoNaoTemEscopo() {
        // A consulta de ativos não devolve o ministério inativo que ela lidera.
        when(ministerioRepository.findAtivosLideradosOuCoordenados(matrizId, pessoaId)).thenReturn(List.of());

        assertThat(service.ministeriosLideradosEm(matrizId)).isEmpty();
    }

    @Test
    void liderDeBaseHerdaSoMinisteriosAtivosDaBaseAtiva() {
        UUID baseId = UUID.randomUUID();
        BaseMinisterial base = new BaseMinisterial();
        base.setId(baseId);
        when(baseRepository.findAtivasLideradasOuCoordenadas(matrizId, pessoaId)).thenReturn(List.of(base));
        when(ministerioRepository.findByBaseMinisterialIdInAndAtivoTrue(Set.of(baseId))).thenReturn(List.of());

        assertThat(service.ministeriosLideradosEm(matrizId)).isEmpty();
        verify(ministerioRepository).findByBaseMinisterialIdInAndAtivoTrue(Set.of(baseId));
    }

    @Test
    void liderSoDeBaseInativaNaoHerdaMinisterios() {
        when(baseRepository.findAtivasLideradasOuCoordenadas(matrizId, pessoaId)).thenReturn(List.of());

        Permissoes p = service.doUsuarioAtual();

        assertThat(p.basesLideradas()).isEmpty();
        assertThat(p.ministeriosLiderados()).isEmpty();
        verify(ministerioRepository, never()).findByBaseMinisterialIdInAndAtivoTrue(any());
    }

    @Test
    void liderSoDeGrupoInativoNaoTemEscopo() {
        when(grupoRepository.findByIgrejaIdAndLiderPessoaIdAndAtivoTrue(matrizId, pessoaId)).thenReturn(List.of());

        assertThat(service.gruposLideradosEm(matrizId)).isEmpty();
    }
}
