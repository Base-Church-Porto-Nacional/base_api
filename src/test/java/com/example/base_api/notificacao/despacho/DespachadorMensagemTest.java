package com.example.base_api.notificacao.despacho;

import com.example.base_api.aniversario.entity.AniversarioEnvio;
import com.example.base_api.aniversario.entity.StatusEnvioAniversario;
import com.example.base_api.notificacao.CanalMensagem;
import com.example.base_api.notificacao.evento.EventoMensagem;
import com.example.base_api.notificacao.produtor.PublicadorMensagem;
import com.example.base_api.pessoa.entity.Pessoa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DespachadorMensagemTest {

    @Mock
    private CanalMensagem canalMensagem;
    @Mock
    private PublicadorMensagem publicador;

    private AniversarioEnvio envio;
    private Pessoa pessoa;

    @BeforeEach
    void setUp() {
        pessoa = new Pessoa();
        pessoa.setId(UUID.randomUUID());
        pessoa.setNome("Maria");
        pessoa.setWhatsapp("5511999994444");

        envio = new AniversarioEnvio();
        envio.setId(UUID.randomUUID());
        envio.setPessoaId(pessoa.getId());
        envio.setDataAniversario(LocalDate.of(2026, 5, 20));
        envio.setStatus(StatusEnvioAniversario.PENDENTE);
    }

    @Test
    void despachadorSincronoEntregaDiretoNoCanal() {
        DespachadorMensagem despachador = new DespachadorSincrono(canalMensagem);

        DespachadorMensagem.Resultado resultado =
                despachador.despachar(envio, pessoa, "Feliz aniversário!");

        assertThat(resultado.status()).isEqualTo(StatusEnvioAniversario.ENVIADO);
        assertThat(resultado.erro()).isNull();
        ArgumentCaptor<CanalMensagem.EnvioMensagem> captor =
                ArgumentCaptor.forClass(CanalMensagem.EnvioMensagem.class);
        verify(canalMensagem).enviar(captor.capture());
        assertThat(captor.getValue().whatsapp()).isEqualTo("5511999994444");
        assertThat(captor.getValue().mensagem()).isEqualTo("Feliz aniversário!");
    }

    @Test
    void despachadorSincronoDevolveFalhaComMensagemDoCanal() {
        doThrow(new IllegalStateException("WhatsApp do membro não informado ou inválido"))
                .when(canalMensagem).enviar(any());
        DespachadorMensagem despachador = new DespachadorSincrono(canalMensagem);

        DespachadorMensagem.Resultado resultado =
                despachador.despachar(envio, pessoa, "Feliz aniversário!");

        assertThat(resultado.status()).isEqualTo(StatusEnvioAniversario.FALHA);
        assertThat(resultado.erro()).isEqualTo("WhatsApp do membro não informado ou inválido");
    }

    @Test
    void despachadorRabbitPublicaEDeveixaPendente() {
        DespachadorMensagem despachador = new DespachadorRabbit(publicador);

        DespachadorMensagem.Resultado resultado =
                despachador.despachar(envio, pessoa, "Feliz aniversário!");

        assertThat(resultado.status()).isEqualTo(StatusEnvioAniversario.PENDENTE);
        assertThat(resultado.erro()).isNull();

        ArgumentCaptor<EventoMensagem> captor = ArgumentCaptor.forClass(EventoMensagem.class);
        verify(publicador).publicar(captor.capture());
        EventoMensagem evento = captor.getValue();
        assertThat(evento.envioId()).isEqualTo(envio.getId());
        assertThat(evento.pessoaId()).isEqualTo(pessoa.getId());
        assertThat(evento.whatsapp()).isEqualTo("5511999994444");
        assertThat(evento.mensagem()).isEqualTo("Feliz aniversário!");
    }

    @Test
    void despachadorRabbitDevolveFalhaSePublicacaoRecusada() {
        doThrow(new IllegalArgumentException("exchange inexistente"))
                .when(publicador).publicar(any());
        DespachadorMensagem despachador = new DespachadorRabbit(publicador);

        DespachadorMensagem.Resultado resultado =
                despachador.despachar(envio, pessoa, "Feliz aniversário!");

        assertThat(resultado.status()).isEqualTo(StatusEnvioAniversario.FALHA);
        assertThat(resultado.erro()).isEqualTo("exchange inexistente");
    }

    }