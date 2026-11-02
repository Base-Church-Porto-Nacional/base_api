package com.example.base_api.usuario.service;

import com.example.base_api.common.exception.LinkExpiradoException;
import com.example.base_api.usuario.entity.TipoTokenAcesso;
import com.example.base_api.usuario.entity.TokenAcesso;
import com.example.base_api.usuario.repository.TokenAcessoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenAcessoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-02T12:00:00Z");

    @Mock
    private TokenAcessoRepository repository;

    private TokenAcessoService service;
    private final UUID usuarioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TokenAcessoService(repository, Clock.fixed(AGORA, ZoneOffset.UTC), "http://localhost:3000/");
    }

    private TokenAcesso gerarCapturando(TipoTokenAcesso tipo, String[] bruto) {
        when(repository.save(any(TokenAcesso.class))).thenAnswer(i -> i.getArgument(0));
        bruto[0] = service.gerar(usuarioId, tipo, null);
        ArgumentCaptor<TokenAcesso> captor = ArgumentCaptor.forClass(TokenAcesso.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void gravaSoOHash() {
        String[] bruto = new String[1];
        TokenAcesso salvo = gerarCapturando(TipoTokenAcesso.CONVITE, bruto);

        assertThat(salvo.getTokenHash()).isNotEqualTo(bruto[0]).matches("[0-9a-f]{64}");
        assertThat(salvo.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(salvo.getTipo()).isEqualTo(TipoTokenAcesso.CONVITE);
    }

    @Test
    void tokenTem43CaracteresBase64Url() {
        String[] bruto = new String[1];
        gerarCapturando(TipoTokenAcesso.CONVITE, bruto);

        assertThat(bruto[0]).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    void conviteValeSeteDias() {
        String[] bruto = new String[1];
        TokenAcesso salvo = gerarCapturando(TipoTokenAcesso.CONVITE, bruto);

        assertThat(salvo.getExpiraEm()).isEqualTo(AGORA.plus(Duration.ofDays(7)));
    }

    @Test
    void redefinicaoValeUmaHora() {
        String[] bruto = new String[1];
        TokenAcesso salvo = gerarCapturando(TipoTokenAcesso.REDEFINICAO, bruto);

        assertThat(salvo.getExpiraEm()).isEqualTo(AGORA.plus(Duration.ofHours(1)));
    }

    @Test
    void novoTokenInvalidaPendenteDoMesmoTipo() {
        String[] bruto = new String[1];
        gerarCapturando(TipoTokenAcesso.CONVITE, bruto);

        verify(repository).apagarPendentes(usuarioId, TipoTokenAcesso.CONVITE);
    }

    @Test
    void naoInvalidaTipoDiferente() {
        String[] bruto = new String[1];
        gerarCapturando(TipoTokenAcesso.REDEFINICAO, bruto);

        verify(repository, never()).apagarPendentes(usuarioId, TipoTokenAcesso.CONVITE);
        verify(repository).apagarPendentes(usuarioId, TipoTokenAcesso.REDEFINICAO);
    }

    @Test
    void validarAceitaTokenVigente() {
        String[] bruto = new String[1];
        TokenAcesso salvo = gerarCapturando(TipoTokenAcesso.CONVITE, bruto);
        when(repository.findByTokenHash(salvo.getTokenHash())).thenReturn(Optional.of(salvo));

        assertThat(service.validar(bruto[0])).isSameAs(salvo);
    }

    @Test
    void expiradoLancaLinkExpirado() {
        TokenAcesso t = new TokenAcesso();
        t.setExpiraEm(AGORA.minusSeconds(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.validar("qualquer"))
                .isInstanceOf(LinkExpiradoException.class)
                .hasMessage("Este link expirou ou já foi usado. Peça um novo convite.");
    }

    @Test
    void usadoLancaLinkExpirado() {
        TokenAcesso t = new TokenAcesso();
        t.setExpiraEm(AGORA.plusSeconds(60));
        t.setUsadoEm(AGORA.minusSeconds(5));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.validar("qualquer")).isInstanceOf(LinkExpiradoException.class);
    }

    @Test
    void desconhecidoLancaLinkExpirado() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validar("nada")).isInstanceOf(LinkExpiradoException.class);
        assertThatThrownBy(() -> service.validar(null)).isInstanceOf(LinkExpiradoException.class);
    }

    @Test
    void consumirDuasVezesLancaLinkExpirado() {
        TokenAcesso t = new TokenAcesso();
        t.setId(UUID.randomUUID());
        when(repository.marcarUsado(t.getId(), AGORA)).thenReturn(1, 0);

        service.consumir(t);

        assertThatThrownBy(() -> service.consumir(t)).isInstanceOf(LinkExpiradoException.class);
    }

    @Test
    void revogarPendentesApagaTodosOsTipos() {
        service.revogarPendentes(usuarioId);

        verify(repository).apagarPendentes(usuarioId);
    }

    @Test
    void linkSemBarraDupla() {
        assertThat(service.link("abc")).isEqualTo("http://localhost:3000/acesso/abc");
    }
}
