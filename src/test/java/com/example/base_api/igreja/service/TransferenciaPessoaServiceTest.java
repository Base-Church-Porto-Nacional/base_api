package com.example.base_api.igreja.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.baseministerial.entity.BaseMinisterial;
import com.example.base_api.baseministerial.repository.BaseMinisterialRepository;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.smallgroup.entity.SmallGroup;
import com.example.base_api.smallgroup.repository.SmallGroupRepository;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.StatusUsuario;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.entity.UsuarioPapel;
import com.example.base_api.usuario.repository.UsuarioRepository;
import com.example.base_api.usuario.service.UsuarioService;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferenciaPessoaServiceTest {

    private static final String MSG_LIDERANCA =
            "Esta pessoa lidera ministérios ou grupos na igreja atual. Troque a liderança antes de transferir.";

    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private IgrejaRepository igrejaRepository;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private MinisterioRepository ministerioRepository;
    @Mock
    private BaseMinisterialRepository baseMinisterialRepository;
    @Mock
    private SmallGroupRepository smallGroupRepository;
    @Mock
    private PessoaServicoRepository pessoaServicoRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private ParticipacoesFuturasService participacoesFuturasService;

    @InjectMocks
    private TransferenciaPessoaService transferencia;

    private final UUID origem = UUID.randomUUID();
    private final UUID destino = UUID.randomUUID();

    private Pessoa pessoa() {
        Pessoa p = new Pessoa();
        p.setId(UUID.randomUUID());
        p.setNome("Maria");
        p.setIgrejaId(origem);
        return p;
    }

    private Usuario usuarioDe(Pessoa pessoa) {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setPessoaId(pessoa.getId());
        u.setIgrejaId(origem);
        u.setStatus(StatusUsuario.ATIVO);
        u.setTokenVersion(3);
        return u;
    }

    private static void comPapelSistema(Usuario usuario, UUID igrejaId, String nome) {
        Papel papel = new Papel();
        papel.setId(UUID.randomUUID());
        papel.setNome(nome);
        UsuarioPapel up = new UsuarioPapel();
        up.setUsuarioId(usuario.getId());
        up.setPapelId(papel.getId());
        up.setIgrejaId(igrejaId);
        up.setPapel(papel);
        usuario.getPapeisIgreja().add(up);
    }

    @Test
    void transfereUsuarioRemovendoPapeisAntigosEIncrementandoTokenVersion() {
        Pessoa pessoa = pessoa();
        Usuario usuario = usuarioDe(pessoa);
        comPapelSistema(usuario, origem, "LIDER");
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.of(usuario));

        transferencia.transferir(pessoa, destino);

        assertThat(pessoa.getIgrejaId()).isEqualTo(destino);
        verify(pessoaRepository).save(pessoa);
        assertThat(usuario.getIgrejaId()).isEqualTo(destino);
        assertThat(usuario.getTokenVersion()).isEqualTo(4);
        verify(usuarioService).substituirPapeis(usuario, origem, Set.of());
        verify(usuarioRepository).save(usuario);
        verify(usuarioService, never()).assertRestaPastorAtivoSemEle(any());
        verify(auditService).registrar(eq("TRANSFERIR_PESSOA"), eq("pessoa"),
                argThat(d -> d.contains("origem=" + origem) && d.contains("destino=" + destino)));
    }

    @Test
    void transferenciaDesativaVinculosDeServicoDaIgrejaAntiga() {
        Pessoa pessoa = pessoa();
        PessoaServico vinculo = new PessoaServico();
        vinculo.setPessoaId(pessoa.getId());
        vinculo.setAtivo(true);
        when(pessoaServicoRepository.findAtivosDaPessoaNaIgreja(pessoa.getId(), origem)).thenReturn(List.of(vinculo));

        transferencia.transferir(pessoa, destino);

        assertThat(vinculo.getAtivo()).isFalse();
        verify(pessoaServicoRepository).saveAll(List.of(vinculo));
    }

    @Test
    void pessoaSemUsuarioSoMudaDeIgreja() {
        Pessoa pessoa = pessoa();
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());

        transferencia.transferir(pessoa, destino);

        assertThat(pessoa.getIgrejaId()).isEqualTo(destino);
        verify(usuarioService, never()).substituirPapeis(any(), any(), any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void pessoaDeIgrejaNaoVisivelNaoETransferida() {
        Pessoa pessoa = pessoa();
        doThrow(new AccessDeniedException("Sem acesso")).when(autorizacaoService).assertAcesso(origem);

        assertThatThrownBy(() -> transferencia.transferir(pessoa, destino)).isInstanceOf(AccessDeniedException.class);
        assertThat(pessoa.getIgrejaId()).isEqualTo(origem);
        verify(pessoaRepository, never()).save(any());
    }

    @Test
    void pastorTitularNaoETransferido() {
        Pessoa pessoa = pessoa();
        when(igrejaRepository.existsByPastorPessoaId(pessoa.getId())).thenReturn(true);

        assertThatThrownBy(() -> transferencia.transferir(pessoa, destino))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Esta pessoa é o pastor titular da igreja atual.");
        assertThat(pessoa.getIgrejaId()).isEqualTo(origem);
    }

    @Test
    void quemLideraBaseNaoETransferido() {
        Pessoa pessoa = pessoa();
        when(baseMinisterialRepository.findLideradasOuCoordenadas(origem, pessoa.getId()))
                .thenReturn(List.of(new BaseMinisterial()));

        assertThatThrownBy(() -> transferencia.transferir(pessoa, destino))
                .isInstanceOf(ConflictException.class)
                .hasMessage(MSG_LIDERANCA);
        verify(pessoaRepository, never()).save(any());
    }

    @Test
    void quemLideraPequenoGrupoNaoETransferido() {
        Pessoa pessoa = pessoa();
        when(smallGroupRepository.findByIgrejaIdAndLiderPessoaId(origem, pessoa.getId()))
                .thenReturn(List.of(new SmallGroup()));

        assertThatThrownBy(() -> transferencia.transferir(pessoa, destino))
                .isInstanceOf(ConflictException.class)
                .hasMessage(MSG_LIDERANCA);
        verify(pessoaRepository, never()).save(any());
    }

    @Test
    void ultimoPastorAtivoDaIgrejaNaoETransferido() {
        Pessoa pessoa = pessoa();
        Usuario usuario = usuarioDe(pessoa);
        comPapelSistema(usuario, origem, "PASTOR");
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.of(usuario));
        doThrow(new BusinessException("A igreja precisa de ao menos um pastor"))
                .when(usuarioService).assertRestaPastorAtivoSemEle(usuario);

        assertThatThrownBy(() -> transferencia.transferir(pessoa, destino))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A igreja precisa de ao menos um pastor");
        assertThat(pessoa.getIgrejaId()).isEqualTo(origem);
        assertThat(usuario.getIgrejaId()).isEqualTo(origem);
        verify(usuarioService, never()).substituirPapeis(any(), any(), any());
    }

    @Test
    void moverNaoAuditaERetornaOrigem() {
        Pessoa pessoa = pessoa();

        TransferenciaPessoaService.Movimentacao retorno = transferencia.mover(pessoa, destino);

        assertThat(retorno.origemId()).isEqualTo(origem);
        assertThat(pessoa.getIgrejaId()).isEqualTo(destino);
        verify(auditService, never()).registrar(any(), any(), any());
    }

    @Test
    void moverAjustaParticipacoesFuturasNaIgrejaDeOrigem() {
        Pessoa pessoa = pessoa();
        when(participacoesFuturasService.ajustarNaSaida(pessoa.getId(), origem)).thenReturn(3);

        TransferenciaPessoaService.Movimentacao retorno = transferencia.mover(pessoa, destino);

        assertThat(retorno.participacoesAjustadas()).isEqualTo(3);
        verify(participacoesFuturasService).ajustarNaSaida(pessoa.getId(), origem);
    }

    @Test
    void auditoriaDaTransferenciaInformaParticipacoesAjustadas() {
        Pessoa pessoa = pessoa();
        when(participacoesFuturasService.ajustarNaSaida(pessoa.getId(), origem)).thenReturn(2);

        transferencia.transferir(pessoa, destino);

        verify(auditService).registrar(eq("TRANSFERIR_PESSOA"), eq("pessoa"),
                argThat(d -> d.contains("participacoesAjustadas=2")));
    }

    @Test
    void pessoaJaNoDestinoNaoAjustaNada() {
        Pessoa pessoa = pessoa();

        assertThat(transferencia.mover(pessoa, origem)).isNull();
        verify(participacoesFuturasService, never()).ajustarNaSaida(any(), any());
    }

    @Test
    void transferenciaRecusadaNaoAjustaParticipacoes() {
        Pessoa pessoa = pessoa();
        when(igrejaRepository.existsByPastorPessoaId(pessoa.getId())).thenReturn(true);

        assertThatThrownBy(() -> transferencia.transferir(pessoa, destino)).isInstanceOf(ConflictException.class);
        verify(participacoesFuturasService, never()).ajustarNaSaida(any(), any());
    }
}
