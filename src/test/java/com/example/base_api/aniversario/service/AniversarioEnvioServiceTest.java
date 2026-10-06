package com.example.base_api.aniversario.service;

import com.example.base_api.aniversario.config.MensagemAniversarioProperties;
import com.example.base_api.aniversario.entity.AniversarioEnvio;
import com.example.base_api.aniversario.entity.MensagemAniversario;
import com.example.base_api.aniversario.entity.StatusEnvioAniversario;
import com.example.base_api.aniversario.repository.AniversarioEnvioRepository;
import com.example.base_api.aniversario.repository.MensagemAniversarioRepository;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.notificacao.CanalMensagem;
import com.example.base_api.notificacao.despacho.DespachadorSincrono;
import com.example.base_api.notificacao.whatsapp.CanalMensagemWhatsapp;
import com.example.base_api.notificacao.whatsapp.ClienteEvolutionApi;
import com.example.base_api.notificacao.whatsapp.PropriedadesWhatsapp;
import com.example.base_api.pessoa.entity.Pessoa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AniversarioEnvioServiceTest {

    @Mock
    private AniversarioEnvioRepository envioRepository;
    @Mock
    private IgrejaRepository igrejaRepository;
    @Mock
    private CanalMensagem canalMensagem;
    @Mock
    private MensagemAniversarioRepository mensagemRepository;

    private MensagemAniversarioProperties properties;

    private MensagemAniversarioService mensagemService;

    private AniversarioEnvioService envioService;

    private Pessoa pessoa;
    private LocalDate dataAniversario;

    @BeforeEach
    void setUp() {
        properties = new MensagemAniversarioProperties();
        properties.setTemplate("Feliz aniversário, {primeiroNome}! {idade} anos. Com carinho, {igreja}.");
        // Sem mensagem própria cadastrada: vale o template de configuração.
        mensagemService = new MensagemAniversarioService(
                mensagemRepository, igrejaRepository, properties, null, null);
        envioService = new AniversarioEnvioService(
                envioRepository, igrejaRepository, new DespachadorSincrono(canalMensagem),
                new MensagemAniversarioTemplate(), mensagemService
        );
        pessoa = new Pessoa();
        pessoa.setId(UUID.randomUUID());
        pessoa.setNome("Maria das Graças");
        pessoa.setTelefone("11988887777");
        pessoa.setWhatsapp("(11) 99999-4444");
        pessoa.setEmail("maria@email.com");
        pessoa.setIgrejaId(UUID.randomUUID());
        pessoa.setDataNascimento(LocalDate.of(1985, 5, 20));
        dataAniversario = LocalDate.of(2026, 5, 20);
    }

    private void mockIgreja() {
        Igreja igreja = new Igreja();
        igreja.setId(pessoa.getIgrejaId());
        igreja.setNome("Igreja Central");
        when(igrejaRepository.findById(igreja.getId())).thenReturn(Optional.of(igreja));
    }

    @Test
    void deveEnviarMensagemPersonalizadaComNomeERegistrarSucesso() {
        mockIgreja();
        when(envioRepository.save(any(AniversarioEnvio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AniversarioEnvioService.Resultado resultado = envioService.enviarPara(pessoa, dataAniversario);

        assertThat(resultado).isEqualTo(AniversarioEnvioService.Resultado.ENVIADO);

        ArgumentCaptor<CanalMensagem.EnvioMensagem> mensagemCaptor =
                ArgumentCaptor.forClass(CanalMensagem.EnvioMensagem.class);
        verify(canalMensagem).enviar(mensagemCaptor.capture());
        CanalMensagem.EnvioMensagem enviada = mensagemCaptor.getValue();
        assertThat(enviada.nome()).isEqualTo("Maria das Graças");
        assertThat(enviada.mensagem())
                .contains("Maria")
                .doesNotContain("{primeiroNome}")
                .contains("41 anos")
                .contains("Igreja Central");

        ArgumentCaptor<AniversarioEnvio> envioCaptor = ArgumentCaptor.forClass(AniversarioEnvio.class);
        verify(envioRepository, atLeastOnce()).save(envioCaptor.capture());
        AniversarioEnvio registro = envioCaptor.getValue();
        assertThat(registro.getPessoaId()).isEqualTo(pessoa.getId());
        assertThat(registro.getIgrejaId()).isEqualTo(pessoa.getIgrejaId());
        assertThat(registro.getDataAniversario()).isEqualTo(dataAniversario);
        assertThat(registro.getStatus()).isEqualTo(StatusEnvioAniversario.ENVIADO);
        assertThat(registro.getDataProcessamento()).isNotNull();
        assertThat(registro.getErro()).isNull();
    }

    @Test
    void deveRegistrarFalhaComOMensagemDeErroDoCanal() {
        mockIgreja();
        doThrow(new RuntimeException("WhatsApp recusou o envio"))
                .when(canalMensagem).enviar(any(CanalMensagem.EnvioMensagem.class));
        when(envioRepository.save(any(AniversarioEnvio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AniversarioEnvioService.Resultado resultado = envioService.enviarPara(pessoa, dataAniversario);

        assertThat(resultado).isEqualTo(AniversarioEnvioService.Resultado.FALHA);

        ArgumentCaptor<AniversarioEnvio> envioCaptor = ArgumentCaptor.forClass(AniversarioEnvio.class);
        verify(envioRepository, atLeastOnce()).save(envioCaptor.capture());
        AniversarioEnvio registro = envioCaptor.getValue();
        assertThat(registro.getStatus()).isEqualTo(StatusEnvioAniversario.FALHA);
        assertThat(registro.getErro()).isEqualTo("WhatsApp recusou o envio");
        assertThat(registro.getMensagem()).contains("Maria");
        assertThat(registro.getDataProcessamento()).isNotNull();
    }

    @Test
    void deveUsarMensagemConfiguradaAoInvésDoPadrão() {
        mockIgreja();
        properties.setTemplate("Parabéns, {nome}, da família {igreja}!");
        when(envioRepository.save(any(AniversarioEnvio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        envioService.enviarPara(pessoa, dataAniversario);

        ArgumentCaptor<CanalMensagem.EnvioMensagem> mensagemCaptor =
                ArgumentCaptor.forClass(CanalMensagem.EnvioMensagem.class);
        verify(canalMensagem).enviar(mensagemCaptor.capture());
        assertThat(mensagemCaptor.getValue().mensagem())
                .isEqualTo("Parabéns, Maria das Graças, da família Igreja Central!");
    }

    @Test
    void deveUsarMensagemDaIgrejaComMarcadoresDoFront() {
        mockIgreja();
        MensagemAniversario daIgreja = new MensagemAniversario();
        daIgreja.setIgrejaId(pessoa.getIgrejaId());
        daIgreja.setMensagem("Feliz aniversário, [Nome]! [Nome Completo], a [Igreja] te ama.");
        when(mensagemRepository.findByIgrejaId(pessoa.getIgrejaId())).thenReturn(Optional.of(daIgreja));
        when(envioRepository.save(any(AniversarioEnvio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        envioService.enviarPara(pessoa, dataAniversario);

        ArgumentCaptor<CanalMensagem.EnvioMensagem> mensagemCaptor =
                ArgumentCaptor.forClass(CanalMensagem.EnvioMensagem.class);
        verify(canalMensagem).enviar(mensagemCaptor.capture());
        assertThat(mensagemCaptor.getValue().mensagem())
                .isEqualTo("Feliz aniversário, Maria! Maria das Graças, a Igreja Central te ama.");
    }

    @Test
    void deveUsarOMensagemPadraoQuandoNenhumaMensagemForConfigurada() {
        mockIgreja();
        properties.setTemplate(null);
        when(envioRepository.save(any(AniversarioEnvio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        envioService.enviarPara(pessoa, dataAniversario);

        ArgumentCaptor<CanalMensagem.EnvioMensagem> mensagemCaptor =
                ArgumentCaptor.forClass(CanalMensagem.EnvioMensagem.class);
        verify(canalMensagem).enviar(mensagemCaptor.capture());
        assertThat(mensagemCaptor.getValue().mensagem())
                .contains("Feliz aniversário, Maria!")
                .contains("Igreja Central");
    }

    @Test
    void devePropagarViolacaoDeUnicidadeComoSinalDeEnvioDuplicado() {
        mockIgreja();
        when(envioRepository.save(any(AniversarioEnvio.class)))
                .thenThrow(new DataIntegrityViolationException("uq_aniversario_envio"));

        assertThatThrownBy(() -> envioService.enviarPara(pessoa, dataAniversario))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRegistrarFALHAQuandoOMembroNaoTemWhatsappValidoNoCanalWhatsapp() {
        mockIgreja();
        properties.setTemplate("Feliz aniversário, {primeiroNome}!");
        pessoa.setWhatsapp(null);

        PropriedadesWhatsapp propriedadesWhatsapp = new PropriedadesWhatsapp();
        propriedadesWhatsapp.setHabilitado(true);
        propriedadesWhatsapp.setUrlBase("http://localhost:8080");
        propriedadesWhatsapp.setApiKey("chave");
        propriedadesWhatsapp.setInstancia("instancia");
        ClienteEvolutionApi cliente = mock(ClienteEvolutionApi.class);

        AniversarioEnvioService servicoComCanalReal = new AniversarioEnvioService(
                envioRepository, igrejaRepository,
                new DespachadorSincrono(new CanalMensagemWhatsapp(cliente, propriedadesWhatsapp)),
                new MensagemAniversarioTemplate(), mensagemService
        );
        when(envioRepository.save(any(AniversarioEnvio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AniversarioEnvioService.Resultado resultado = servicoComCanalReal.enviarPara(pessoa, dataAniversario);

        assertThat(resultado).isEqualTo(AniversarioEnvioService.Resultado.FALHA);
        verify(cliente, never()).enviarTexto(any(String.class), any(String.class));

        ArgumentCaptor<AniversarioEnvio> envioCaptor = ArgumentCaptor.forClass(AniversarioEnvio.class);
        verify(envioRepository, atLeastOnce()).save(envioCaptor.capture());
        assertThat(envioCaptor.getValue().getStatus()).isEqualTo(StatusEnvioAniversario.FALHA);
        assertThat(envioCaptor.getValue().getErro()).contains("WhatsApp do membro não informado");
    }
}