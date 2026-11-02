package com.example.base_api.usuario.service;

import com.example.base_api.audit.service.AuditService;
import com.example.base_api.common.exception.ConflictException;
import com.example.base_api.common.exception.EntidadeNaoProcessavelException;
import com.example.base_api.common.exception.LinkExpiradoException;
import com.example.base_api.common.exception.TooManyRequestsException;
import com.example.base_api.security.LoginRateLimiter;
import com.example.base_api.usuario.dto.DefinirSenhaRequest;
import com.example.base_api.usuario.dto.LoginResponse;
import com.example.base_api.usuario.dto.TokenInfoResponse;
import com.example.base_api.usuario.entity.TokenAcesso;
import jakarta.validation.Validation;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Mock
    private PasswordEncoder passwordEncoder;

    private final java.util.List<Runnable> filaExecutor = new java.util.ArrayList<>();
    private LoginRateLimiter rateLimiter;
    private AcessoService service;

    private final UUID igrejaId = UUID.randomUUID();
    private final UUID atorId = UUID.randomUUID();
    private Pessoa pessoa;

    @BeforeEach
    void setUp() {
        MensagemAcesso mensagens = new MensagemAcesso(igrejaRepository, canalMensagem);
        EnvioRedefinicaoService envioRedefinicao = new EnvioRedefinicaoService(
                usuarioRepository, pessoaService, tokenAcessoService, mensagens);
        service = new AcessoService(pessoaService, usuarioRepository, tokenAcessoService,
                mensagens, envioRedefinicao, filaExecutor::add, autorizacaoService, usuarioService, auditService,
                passwordEncoder, rateLimiter = new LoginRateLimiter());
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

    private static final String IP = "10.0.0.1";

    private TokenAcesso tokenDe(Usuario u, TipoTokenAcesso tipo) {
        TokenAcesso t = new TokenAcesso();
        t.setId(UUID.randomUUID());
        t.setUsuarioId(u.getId());
        t.setTipo(tipo);
        return t;
    }

    private void stubIgrejaEPessoa() {
        Igreja igreja = new Igreja();
        igreja.setId(igrejaId);
        igreja.setNome("Igreja Central");
        when(igrejaRepository.findById(igrejaId)).thenReturn(Optional.of(igreja));
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);
    }

    @Test
    void definirSenhaAtivaContaConvidadaEDevolveLogin() {
        Usuario u = usuarioExistente(StatusUsuario.CONVIDADO);
        u.setTokenVersion(0);
        TokenAcesso t = tokenDe(u, TipoTokenAcesso.CONVITE);
        when(tokenAcessoService.validar(TOKEN)).thenReturn(t);
        when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("senhaforte1")).thenReturn("HASH");
        when(usuarioRepository.save(u)).thenReturn(u);
        LoginResponse login = new LoginResponse("jwt", "Bearer", u.getId(), u.getPessoaId(), igrejaId, null);
        when(usuarioService.emitirLogin(u)).thenReturn(login);

        LoginResponse r = service.definirSenha(new DefinirSenhaRequest(TOKEN, "senhaforte1"), IP);

        assertThat(r.token()).isNotBlank();
        assertThat(u.getStatus()).isEqualTo(StatusUsuario.ATIVO);
        assertThat(u.getSenhaHash()).isEqualTo("HASH");
        assertThat(u.getTokenVersion()).isEqualTo(1);
        verify(tokenAcessoService).consumir(t);
        verify(tokenAcessoService).revogarPendentes(u.getId());
        verify(auditService).registrar(eq("DEFINIR_SENHA"), eq("usuario"), any());
    }

    @Test
    void redefinicaoInvalidaSessoesAnteriores() {
        Usuario u = usuarioExistente(StatusUsuario.ATIVO);
        u.setTokenVersion(3);
        when(tokenAcessoService.validar(TOKEN)).thenReturn(tokenDe(u, TipoTokenAcesso.REDEFINICAO));
        when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
        when(passwordEncoder.encode(any())).thenReturn("HASH");
        when(usuarioRepository.save(u)).thenReturn(u);

        service.definirSenha(new DefinirSenhaRequest(TOKEN, "senhaforte1"), IP);

        assertThat(u.getTokenVersion()).isEqualTo(4);
        assertThat(u.getStatus()).isEqualTo(StatusUsuario.ATIVO);
    }

    @Test
    void definirSenhaComTokenJaUsadoLanca410() {
        when(tokenAcessoService.validar(TOKEN)).thenThrow(new LinkExpiradoException("Link expirado"));

        assertThatThrownBy(() -> service.definirSenha(new DefinirSenhaRequest(TOKEN, "senhaforte1"), IP))
                .isInstanceOf(LinkExpiradoException.class);
        verify(usuarioRepository, never()).save(any());
        verify(usuarioService, never()).emitirLogin(any());
    }

    @Test
    void definirSenhaComConsumoPerdidoNaoGravaSenha() {
        Usuario u = usuarioExistente(StatusUsuario.CONVIDADO);
        TokenAcesso t = tokenDe(u, TipoTokenAcesso.CONVITE);
        when(tokenAcessoService.validar(TOKEN)).thenReturn(t);
        when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
        doThrow(new LinkExpiradoException("Link expirado")).when(tokenAcessoService).consumir(t);

        assertThatThrownBy(() -> service.definirSenha(new DefinirSenhaRequest(TOKEN, "senhaforte1"), IP))
                .isInstanceOf(LinkExpiradoException.class);
        verify(usuarioRepository, never()).save(any());
        assertThat(u.getSenhaHash()).isNull();
    }

    @Test
    void definirSenhaDeContaDesativadaLanca410SemConsumir() {
        Usuario u = usuarioExistente(StatusUsuario.DESATIVADO);
        TokenAcesso t = tokenDe(u, TipoTokenAcesso.REDEFINICAO);
        when(tokenAcessoService.validar(TOKEN)).thenReturn(t);
        when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.definirSenha(new DefinirSenhaRequest(TOKEN, "senhaforte1"), IP))
                .isInstanceOf(LinkExpiradoException.class);
        verify(tokenAcessoService, never()).consumir(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void definirSenhaComSenhaCurtaFalhaValidacao() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violacoes = factory.getValidator().validate(new DefinirSenhaRequest(TOKEN, "1234567"));
            assertThat(violacoes).hasSize(1);
            assertThat(factory.getValidator().validate(new DefinirSenhaRequest(TOKEN, "12345678"))).isEmpty();
        }
    }

    @Test
    void definirSenhaComLinkInvalidoRespeitaRateLimit() {
        when(tokenAcessoService.validar(TOKEN)).thenThrow(new LinkExpiradoException("Link expirado"));
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.definirSenha(new DefinirSenhaRequest(TOKEN, "senhaforte1"), IP))
                    .isInstanceOf(LinkExpiradoException.class);
        }
        assertThatThrownBy(() -> service.definirSenha(new DefinirSenhaRequest(TOKEN, "senhaforte1"), IP))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void infoDevolvePrimeiroNomeETipo() {
        Usuario u = usuarioExistente(StatusUsuario.CONVIDADO);
        when(tokenAcessoService.validar(TOKEN)).thenReturn(tokenDe(u, TipoTokenAcesso.CONVITE));
        when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
        when(pessoaService.buscarEntidade(pessoa.getId())).thenReturn(pessoa);

        TokenInfoResponse r = service.info(TOKEN, IP);

        assertThat(r.tipo()).isEqualTo(TipoTokenAcesso.CONVITE);
        assertThat(r.primeiroNome()).isEqualTo("Mari");
    }

    @Test
    void infoComTokenInvalidoLanca410ERegistraFalha() {
        when(tokenAcessoService.validar("x")).thenThrow(new LinkExpiradoException("Link expirado"));
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.info("x", IP)).isInstanceOf(LinkExpiradoException.class);
        }
        assertThatThrownBy(() -> service.info("x", IP)).isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void esqueciSenhaContaAtivaComTelefoneEnvia() {
        Usuario u = usuarioExistente(StatusUsuario.ATIVO);
        u.setTelefone("5563999990000");
        stubIgrejaEPessoa();
        when(usuarioRepository.findByIdentificador("(63) 99999-0000")).thenReturn(Optional.of(u));
        when(tokenAcessoService.gerar(u.getId(), TipoTokenAcesso.REDEFINICAO, null)).thenReturn(TOKEN);
        when(tokenAcessoService.link(TOKEN)).thenReturn(LINK);

        when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));

        service.solicitarRedefinicao("(63) 99999-0000", IP);
        filaExecutor.forEach(Runnable::run);

        ArgumentCaptor<EnvioMensagem> envio = ArgumentCaptor.forClass(EnvioMensagem.class);
        verify(canalMensagem).enviar(envio.capture());
        assertThat(envio.getValue().whatsapp()).isEqualTo("5563999990000");
        assertThat(envio.getValue().mensagem()).isEqualTo(
                "Olá, Mari! Para criar uma nova senha do app da Igreja Central, use este link (vale por 1 hora): " + LINK);
    }

    @Test
    void esqueciSenhaIdentificadorDesconhecidoNaoEnviaNada() {
        when(usuarioRepository.findByIdentificador("x@y.com")).thenReturn(Optional.empty());

        service.solicitarRedefinicao("x@y.com", IP);

        verify(canalMensagem, never()).enviar(any());
        verify(tokenAcessoService, never()).gerar(any(), any(), any());
    }

    @Test
    void esqueciSenhaContaConvidadaNaoEnvia() {
        Usuario u = usuarioExistente(StatusUsuario.CONVIDADO);
        u.setTelefone("5563999990000");
        when(usuarioRepository.findByIdentificador("a@b.com")).thenReturn(Optional.of(u));

        service.solicitarRedefinicao("a@b.com", IP);

        verify(canalMensagem, never()).enviar(any());
        verify(tokenAcessoService, never()).gerar(any(), any(), any());
    }

    @Test
    void esqueciSenhaContaDesativadaNaoEnvia() {
        Usuario u = usuarioExistente(StatusUsuario.DESATIVADO);
        u.setTelefone("5563999990000");
        when(usuarioRepository.findByIdentificador("a@b.com")).thenReturn(Optional.of(u));

        service.solicitarRedefinicao("a@b.com", IP);

        verify(canalMensagem, never()).enviar(any());
        verify(tokenAcessoService, never()).gerar(any(), any(), any());
    }

    @Test
    void esqueciSenhaContaSemTelefoneNaoEnvia() {
        Usuario u = usuarioExistente(StatusUsuario.ATIVO);
        when(usuarioRepository.findByIdentificador("a@b.com")).thenReturn(Optional.of(u));

        service.solicitarRedefinicao("a@b.com", IP);

        verify(canalMensagem, never()).enviar(any());
        verify(tokenAcessoService, never()).gerar(any(), any(), any());
    }

    @Test
    void esqueciSenhaRespeitaRateLimit() {
        when(usuarioRepository.findByIdentificador("a@b.com")).thenReturn(Optional.empty());
        for (int i = 0; i < 5; i++) {
            service.solicitarRedefinicao("a@b.com", IP);
        }
        assertThatThrownBy(() -> service.solicitarRedefinicao("A@B.com", IP))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void esqueciSenhaNaoGeraTokenNaThreadDaRequisicao() {
        Usuario u = usuarioExistente(StatusUsuario.ATIVO);
        u.setTelefone("5563999990000");
        when(usuarioRepository.findByIdentificador("a@b.com")).thenReturn(Optional.of(u));

        service.solicitarRedefinicao("a@b.com", IP);

        verify(tokenAcessoService, never()).gerar(any(), any(), any());
        verify(canalMensagem, never()).enviar(any());
        assertThat(filaExecutor).hasSize(1);
    }

    @Test
    void esqueciSenhaComFalhaNoEnvioAssincronoNaoPropaga() {
        Usuario u = usuarioExistente(StatusUsuario.ATIVO);
        u.setTelefone("5563999990000");
        when(usuarioRepository.findByIdentificador("a@b.com")).thenReturn(Optional.of(u));
        when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
        when(pessoaService.buscarEntidade(pessoa.getId())).thenThrow(new IllegalStateException("boom"));

        service.solicitarRedefinicao("a@b.com", IP);

        org.assertj.core.api.Assertions.assertThatCode(() -> filaExecutor.forEach(Runnable::run)).doesNotThrowAnyException();
    }

    @Test
    void esqueciSenhaRunnableCapturaFalhaDoEnvioProxiado() {
        EnvioRedefinicaoService envio = org.mockito.Mockito.mock(EnvioRedefinicaoService.class);
        org.mockito.Mockito.doThrow(new org.springframework.transaction.UnexpectedRollbackException("rollback"))
                .when(envio).gerarEEnviar(any());
        AcessoService servico = new AcessoService(pessoaService, usuarioRepository, tokenAcessoService,
                new MensagemAcesso(igrejaRepository, canalMensagem), envio, filaExecutor::add,
                autorizacaoService, usuarioService, auditService, passwordEncoder, new LoginRateLimiter());
        Usuario u = usuarioExistente(StatusUsuario.ATIVO);
        u.setTelefone("5563999990000");
        when(usuarioRepository.findByIdentificador("a@b.com")).thenReturn(Optional.of(u));

        servico.solicitarRedefinicao("a@b.com", IP);

        assertThat(filaExecutor).hasSize(1);
        org.assertj.core.api.Assertions.assertThatCode(() -> filaExecutor.forEach(Runnable::run)).doesNotThrowAnyException();
        verify(envio).gerarEEnviar(u.getId());
    }

    @Test
    void esqueciNaoCompartilhaContadorComLogin() {
        String telefone = "(63) 99999-0000";
        when(usuarioRepository.findByIdentificador(telefone)).thenReturn(Optional.empty());
        for (int i = 0; i < 5; i++) {
            service.solicitarRedefinicao(telefone, IP);
        }
        // contador de esqueci esgotado, mas login e token seguem livres
        org.assertj.core.api.Assertions.assertThatCode(() -> {
            rateLimiter.assertPermitido(telefone, IP);
            rateLimiter.assertPermitido("token", "token", IP);
        }).doesNotThrowAnyException();
        for (int i = 0; i < 5; i++) {
            rateLimiter.registrarFalha(telefone, IP);
        }
        assertThatThrownBy(() -> rateLimiter.assertPermitido(telefone, IP)).isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void infoComPessoaInexistenteLanca410() {
        Usuario u = usuarioExistente(StatusUsuario.CONVIDADO);
        when(tokenAcessoService.validar(TOKEN)).thenReturn(tokenDe(u, TipoTokenAcesso.CONVITE));
        when(usuarioRepository.findById(u.getId())).thenReturn(Optional.of(u));
        when(pessoaService.buscarEntidade(pessoa.getId()))
                .thenThrow(new com.example.base_api.common.exception.ResourceNotFoundException("Pessoa não encontrada"));

        assertThatThrownBy(() -> service.info(TOKEN, IP)).isInstanceOf(LinkExpiradoException.class);
    }
}
