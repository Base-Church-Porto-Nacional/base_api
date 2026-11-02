package com.example.base_api.usuario.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.common.exception.EntidadeNaoProcessavelException;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.notificacao.CanalMensagem;
import com.example.base_api.notificacao.CanalMensagem.EnvioMensagem;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.service.PessoaService;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.usuario.dto.LinkAcessoResponse;
import com.example.base_api.usuario.entity.StatusUsuario;
import com.example.base_api.usuario.entity.TipoTokenAcesso;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcessoServiceTest {

    private static final String TOKEN = "tokenbruto";
    private static final String LINK = "http://localhost:3000/acesso/tokenbruto";

    @Mock
    private PessoaService pessoaService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private IgrejaRepository igrejaRepository;
    @Mock
    private TokenAcessoService tokenAcessoService;
    @Mock
    private CanalMensagem canalMensagem;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private AuditService auditService;

    private AcessoService service;

    private final UUID igrejaId = UUID.randomUUID();
    private final UUID atorId = UUID.randomUUID();
    private Pessoa pessoa;

    @BeforeEach
    void setUp() {
        service = new AcessoService(pessoaService, usuarioRepository, igrejaRepository, tokenAcessoService,
                canalMensagem, autorizacaoService, usuarioService, auditService);
        pessoa = new Pessoa();
        pessoa.setId(UUID.randomUUID());
        pessoa.setNome("Maria da Silva");
        pessoa.setNomeSocial("Mari Silva");
        pessoa.setEmail("Maria@Email.com ");
        pessoa.setWhatsapp("(63) 99999-0000");
        pessoa.setIgrejaId(igrejaId);
    }

    private void stubCaminhoFeliz() {
        Usuario ator = new Usuario();
        ator.setId(atorId);
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        when(autorizacaoService.requireUsuario()).thenReturn(ator);
        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setNome("Igreja Central");
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.of(igreja));
        when(tokenAcessoService.gerar(any(), any(), any())).thenReturn(TOKEN);
        when(tokenAcessoService.link(TOKEN)).thenReturn(LINK);
    }

    private Usuario usuarioExistente(StatusUsuario status) {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setPessoaId(pessoa.getId());
        u.setIgrejaId(igrejaId);
        u.setStatus(status);
        return u;
    }

    @Test
    void conviteCriaUsuarioConvidadoSemSenhaEEnviaWhatsapp() {
        stubCaminhoFeliz();
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        LinkAcessoResponse resposta = service.convidar(pessoa.getId());

        assertThat(resposta.link()).isEqualTo(LINK);
        assertThat(resposta.enviadoWhatsapp()).isTrue();
        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuario.capture());
        assertThat(usuario.getValue().getStatus()).isEqualTo(StatusUsuario.CONVIDADO);
        assertThat(usuario.getValue().getSenhaHash()).isNull();
        assertThat(usuario.getValue().getEmail()).isEqualTo("maria@email.com");
        assertThat(usuario.getValue().getTelefone()).isEqualTo("5563999990000");
        assertThat(usuario.getValue().getIgrejaId()).isEqualTo(igrejaId);
        assertThat(usuario.getValue().getPessoaId()).isEqualTo(pessoa.getId());
        assertThat(usuario.getValue().getPapeisIgreja()).isEmpty();

        ArgumentCaptor<EnvioMensagem> envio = ArgumentCaptor.forClass(EnvioMensagem.class);
        verify(canalMensagem).enviar(envio.capture());
        assertThat(envio.getValue().whatsapp()).isEqualTo("5563999990000");
        assertThat(envio.getValue().mensagem()).isEqualTo(
                "Olá, Mari! Você foi convidado para usar o app da Igreja Central. "
                        + "Crie sua senha neste link (vale por 7 dias): " + LINK);
        verify(tokenAcessoService).gerar(any(), eq(TipoTokenAcesso.CONVITE), eq(atorId));
        verify(auditService).registrar(eq("CONVIDAR"), eq("usuario"), any());
    }

    @Test
    void conviteComCanalFalhandoDevolveLinkComEnviadoFalse() {
        stubCaminhoFeliz();
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new IllegalStateException("sem canal")).when(canalMensagem).enviar(any());

        LinkAcessoResponse resposta = service.convidar(pessoa.getId());

        assertThat(resposta.link()).isEqualTo(LINK);
        assertThat(resposta.enviadoWhatsapp()).isFalse();
    }

    @Test
    void conviteSemContatoLanca422() {
        pessoa.setEmail(null);
        pessoa.setWhatsapp(null);
        pessoa.setTelefone(" ");
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);

        assertThatThrownBy(() -> service.convidar(pessoa.getId()))
                .isInstanceOf(EntidadeNaoProcessavelException.class)
                .hasMessage("Cadastre e-mail ou WhatsApp antes de convidar");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void conviteSoComEmailNaoEnviaWhatsapp() {
        pessoa.setWhatsapp(null);
        pessoa.setTelefone(null);
        stubCaminhoFeliz();
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        LinkAcessoResponse resposta = service.convidar(pessoa.getId());

        assertThat(resposta.enviadoWhatsapp()).isFalse();
        verify(canalMensagem, never()).enviar(any());
    }

    @Test
    void conviteComTelefoneDeOutraContaLanca409ENaoSalva() {
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByTelefone("5563999990000")).thenReturn(true);

        assertThatThrownBy(() -> service.convidar(pessoa.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Telefone já usado por outra conta");
        verify(usuarioRepository, never()).save(any());
        verify(tokenAcessoService, never()).gerar(any(), any(), any());
    }

    @Test
    void conviteComEmailDeOutraContaLanca409ENaoSalva() {
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("maria@email.com")).thenReturn(Optional.of(usuarioExistente(StatusUsuario.ATIVO)));

        assertThatThrownBy(() -> service.convidar(pessoa.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("E-mail já usado por outra conta");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void reenvioParaConvidadoGeraNovoTokenSemNovoUsuario() {
        stubCaminhoFeliz();
        Usuario convidado = usuarioExistente(StatusUsuario.CONVIDADO);
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.of(convidado));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        LinkAcessoResponse resposta = service.convidar(pessoa.getId());

        assertThat(resposta.enviadoWhatsapp()).isTrue();
        verify(tokenAcessoService).gerar(convidado.getId(), TipoTokenAcesso.CONVITE, atorId);
    }

    @Test
    void reenvioAtualizaTelefoneDoConvidado() {
        stubCaminhoFeliz();
        Usuario convidado = usuarioExistente(StatusUsuario.CONVIDADO);
        convidado.setTelefone("5563888880000");
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.of(convidado));
        when(usuarioRepository.findByTelefone("5563999990000")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        service.convidar(pessoa.getId());

        assertThat(convidado.getTelefone()).isEqualTo("5563999990000");
        assertThat(convidado.getEmail()).isEqualTo("maria@email.com");
        verify(usuarioRepository).save(convidado);
    }

    @Test
    void reenvioComTelefoneDeOutraContaLanca409() {
        Usuario convidado = usuarioExistente(StatusUsuario.CONVIDADO);
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.of(convidado));
        Usuario outro = new Usuario();
        outro.setId(UUID.randomUUID());
        when(usuarioRepository.findByTelefone("5563999990000")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> service.convidar(pessoa.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Telefone já usado por outra conta");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void conviteParaAtivoLanca409() {
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        when(usuarioRepository.findByPessoaId(pessoa.getId())).thenReturn(Optional.of(usuarioExistente(StatusUsuario.ATIVO)));

        assertThatThrownBy(() -> service.convidar(pessoa.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Esta pessoa já tem acesso ao app");
    }

    @Test
    void conviteParaDesativadoLanca409() {
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        when(usuarioRepository.findByPessoaId(pessoa.getId()))
                .thenReturn(Optional.of(usuarioExistente(StatusUsuario.DESATIVADO)));

        assertThatThrownBy(() -> service.convidar(pessoa.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A conta está desativada. Reative antes de convidar");
    }

    @Test
    void conviteSemPermissaoLanca403() {
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        doThrow(new AccessDeniedException("x")).when(autorizacaoService).assertPodeConvidar(pessoa);

        assertThatThrownBy(() -> service.convidar(pessoa.getId())).isInstanceOf(AccessDeniedException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void secretariaNaoGeraRedefinicaoDoPastor() {
        Usuario pastor = usuarioExistente(StatusUsuario.ATIVO);
        when(usuarioRepository.findById(pastor.getId())).thenReturn(Optional.of(pastor));
        doThrow(new AccessDeniedException("Somente o pastor")).when(usuarioService).assertPodeAlterarContaDeGestao(pastor);

        assertThatThrownBy(() -> service.gerarRedefinicao(pastor.getId())).isInstanceOf(AccessDeniedException.class);
        verify(tokenAcessoService, never()).gerar(any(), any(), any());
    }

    @Test
    void pastorGeraRedefinicaoDaSecretaria() {
        stubCaminhoFeliz();
        Usuario secretaria = usuarioExistente(StatusUsuario.ATIVO);
        when(usuarioRepository.findById(secretaria.getId())).thenReturn(Optional.of(secretaria));
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
        secretaria.setTelefone("5563999990000");

        LinkAcessoResponse resposta = service.gerarRedefinicao(secretaria.getId());

        assertThat(resposta.link()).isEqualTo(LINK);
        assertThat(resposta.enviadoWhatsapp()).isTrue();
        verify(autorizacaoService).assertTem(igrejaId, com.example.base_api.security.Capacidade.CONTAS_GERIR);
        verify(usuarioService).assertPodeAlterarContaDeGestao(secretaria);
        verify(tokenAcessoService).gerar(secretaria.getId(), TipoTokenAcesso.REDEFINICAO, atorId);
        ArgumentCaptor<EnvioMensagem> envio = ArgumentCaptor.forClass(EnvioMensagem.class);
        verify(canalMensagem).enviar(envio.capture());
        assertThat(envio.getValue().mensagem()).isEqualTo(
                "Olá, Mari! Para criar uma nova senha do app da Igreja Central, use este link (vale por 1 hora): " + LINK);
        verify(auditService).registrar(eq("GERAR_REDEFINICAO"), eq("usuario"), any());
    }

    @Test
    void redefinicaoDeContaConvidadaLanca409() {
        Usuario convidado = usuarioExistente(StatusUsuario.CONVIDADO);
        when(usuarioRepository.findById(convidado.getId())).thenReturn(Optional.of(convidado));

        assertThatThrownBy(() -> service.gerarRedefinicao(convidado.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Só é possível redefinir a senha de uma conta ativa");
        verify(tokenAcessoService, never()).gerar(any(), any(), any());
    }
}
