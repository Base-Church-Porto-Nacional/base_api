package com.example.base_api.aniversario.service;

import com.example.base_api.aniversario.config.MensagemAniversarioProperties;
import com.example.base_api.aniversario.entity.AniversarioEnvio;
import com.example.base_api.aniversario.entity.StatusEnvioAniversario;
import com.example.base_api.aniversario.repository.AniversarioEnvioRepository;
import com.example.base_api.audit.service.AuditService;
import com.example.base_api.pessoa.entity.Pessoa;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AniversarioServiceTest {

    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private AniversarioEnvioRepository envioRepository;
    @Mock
    private AniversarioEnvioService envioService;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private AuditService auditService;

    private MensagemAniversarioProperties properties;

    @InjectMocks
    private AniversarioService aniversarioService;

    @BeforeEach
    void setUp() {
        properties = new MensagemAniversarioProperties();
        properties.setAtivo(true);
        properties.setTemplate("Feliz aniversário, {primeiroNome}!");
        aniversarioService = new AniversarioService(
                pessoaRepository, envioRepository, envioService, properties,
                autorizacaoService, auditService
        );
    }

    private Pessoa pessoa(String nome, LocalDate nascimento, UUID igrejaId) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(UUID.randomUUID());
        pessoa.setNome(nome);
        pessoa.setDataNascimento(nascimento);
        pessoa.setIgrejaId(igrejaId);
        pessoa.setAtivo(true);
        return pessoa;
    }

    private Pessoa pessoaAniversariante(UUID igrejaId) {
        return pessoa("Maria", LocalDate.of(1985, 5, 20), igrejaId);
    }

    @Test
    void deveIdentificarAniversariantesPeloDiaEMesDaDataDeNascimento() {
        LocalDate hoje = LocalDate.of(2026, 5, 20);
        when(pessoaRepository.findAniversariantesDoDia(20, 5)).thenReturn(List.of());

        String resumo = aniversarioService.processar(hoje);

        verify(pessoaRepository).findAniversariantesDoDia(20, 5);
        assertThat(resumo).contains("Nenhum aniversariante em 2026-05-20");
    }

    @Test
    void deveEnviarMensagemParaCadaAniversarianteDoDia() {
        LocalDate hoje = LocalDate.of(2026, 5, 20);
        Pessoa maria = pessoa("Maria", LocalDate.of(1985, 5, 20), UUID.randomUUID());
        Pessoa joao = pessoa("João", LocalDate.of(1990, 5, 20), UUID.randomUUID());

        when(pessoaRepository.findAniversariantesDoDia(20, 5)).thenReturn(List.of(maria, joao));
        when(envioRepository.existsByPessoaIdAndDataAniversario(any(), any())).thenReturn(false);
        when(envioService.enviarPara(any(Pessoa.class), any(LocalDate.class)))
                .thenReturn(AniversarioEnvioService.Resultado.ENVIADO);

        String resumo = aniversarioService.processar(hoje);

        assertThat(resumo).contains("2 enviados, 0 enfileirados, 0 ignorados, 0 falhas");
        verify(envioService).enviarPara(maria, hoje);
        verify(envioService).enviarPara(joao, hoje);
    }

    @Test
    void deveIgnorarPessoaSemDataDeNascimento() {
        LocalDate hoje = LocalDate.of(2026, 5, 20);
        Pessoa semData = pessoa("Sem Data", null, UUID.randomUUID());
        Pessoa maria = pessoa("Maria", LocalDate.of(1985, 5, 20), UUID.randomUUID());

        when(pessoaRepository.findAniversariantesDoDia(20, 5)).thenReturn(List.of(semData, maria));
        when(envioRepository.existsByPessoaIdAndDataAniversario(any(), any())).thenReturn(false);
        when(envioService.enviarPara(any(Pessoa.class), any(LocalDate.class)))
                .thenReturn(AniversarioEnvioService.Resultado.ENVIADO);

        String resumo = aniversarioService.processar(hoje);

        assertThat(resumo).contains("1 enviados, 0 enfileirados, 1 ignorados, 0 falhas");
        verify(envioService, never()).enviarPara(semData, hoje);
    }

    @Test
    void naoDeveEnviarNovamenteParaOMesmoAniversario() {
        LocalDate hoje = LocalDate.of(2026, 5, 20);
        Pessoa maria = pessoa("Maria", LocalDate.of(1985, 5, 20), UUID.randomUUID());

        when(pessoaRepository.findAniversariantesDoDia(20, 5)).thenReturn(List.of(maria));
        when(envioRepository.existsByPessoaIdAndDataAniversario(maria.getId(), hoje)).thenReturn(true);

        String resumo = aniversarioService.processar(hoje);

        assertThat(resumo).contains("0 enviados, 0 enfileirados, 1 ignorados, 0 falhas");
        verify(envioService, never()).enviarPara(any(Pessoa.class), any(LocalDate.class));
    }

    @Test
    void deveContarFalhasQuandoOCanalDeEnvioRecusar() {
        LocalDate hoje = LocalDate.of(2026, 5, 20);
        Pessoa maria = pessoa("Maria", LocalDate.of(1985, 5, 20), UUID.randomUUID());

        when(pessoaRepository.findAniversariantesDoDia(20, 5)).thenReturn(List.of(maria));
        when(envioRepository.existsByPessoaIdAndDataAniversario(maria.getId(), hoje)).thenReturn(false);
        when(envioService.enviarPara(maria, hoje)).thenReturn(AniversarioEnvioService.Resultado.FALHA);

        String resumo = aniversarioService.processar(hoje);

        assertThat(resumo).contains("0 enviados, 0 enfileirados, 0 ignorados, 1 falhas");
    }

    @Test
    void deveProcessarAutomaticamenteNaDataDoAniversario() {
        when(pessoaRepository.findAniversariantesDoDia(anyInt(), anyInt())).thenReturn(List.of());

        String resumo = aniversarioService.enviarAniversariantesDoDia();

        LocalDate hoje = LocalDate.now();
        verify(pessoaRepository).findAniversariantesDoDia(hoje.getDayOfMonth(), hoje.getMonthValue());
        assertThat(resumo).contains("Nenhum aniversariante em " + hoje);
    }

    @Test
    void deveRespeitarFlagDeDesativacaoDaMensagem() {
        properties.setAtivo(false);

        String resumo = aniversarioService.enviarAniversariantesDoDia();

        assertThat(resumo).contains("desativada");
        verify(pessoaRepository, never()).findAniversariantesDoDia(anyInt(), anyInt());
    }

    @Test
    void deveAutorizarExecucaoManualNasIgrejasVisiveis() {
        UUID igrejaId = UUID.randomUUID();
        when(autorizacaoService.idsParaListagem(igrejaId)).thenReturn(List.of(igrejaId));
        when(pessoaRepository.findAniversariantesDoDia(anyInt(), anyInt())).thenReturn(List.of());

        String resumo = aniversarioService.enviarAniversariantesDoDiaManual(igrejaId);

        verify(autorizacaoService).assertPastorOuAdmin(igrejaId);
        verify(auditService).registrar(eq("ENVIAR_ANIVERSARIO"), eq("pessoa"), contains("data="));
        assertThat(resumo).isNotBlank();
    }

    @Test
    void execucaoManualNaoPodeEnviarParaIgrejaNaoAutorizada() {
        UUID igrejaVisivel = UUID.randomUUID();
        UUID igrejaAlheia = UUID.randomUUID();

        Pessoa daIgrejaVisivel = pessoaAniversariante(igrejaVisivel);
        Pessoa deIgrejaNaoAutorizada = pessoaAniversariante(igrejaAlheia);

        when(autorizacaoService.idsParaListagem(igrejaVisivel)).thenReturn(List.of(igrejaVisivel));
        when(pessoaRepository.findAniversariantesDoDia(anyInt(), anyInt()))
                .thenReturn(List.of(daIgrejaVisivel, deIgrejaNaoAutorizada));
        when(envioRepository.existsByPessoaIdAndDataAniversario(any(), any())).thenReturn(false);
        when(envioService.enviarPara(any(), any())).thenReturn(AniversarioEnvioService.Resultado.ENVIADO);

        aniversarioService.enviarAniversariantesDoDiaManual(igrejaVisivel);

        verify(envioService).enviarPara(eq(daIgrejaVisivel), any());
        verify(envioService, never()).enviarPara(eq(deIgrejaNaoAutorizada), any());
    }

    @Test
    void naoDeveProcessarQuandoNaoHaIgrejaVisivel() {
        when(autorizacaoService.idsParaListagem(null)).thenReturn(List.of());

        String resumo = aniversarioService.enviarAniversariantesDoDiaManual(null);

        assertThat(resumo).contains("Nenhuma igreja visível");
        verify(pessoaRepository, never()).findAniversariantesDoDia(anyInt(), anyInt());
        verify(envioRepository, never()).save(any(AniversarioEnvio.class));
    }

    @Test
    void deveListarEnviosDasIgrejasVisiveis() {
        UUID igrejaId = UUID.randomUUID();
        AniversarioEnvio envio = new AniversarioEnvio();
        envio.setPessoaId(UUID.randomUUID());
        envio.setIgrejaId(igrejaId);
        envio.setDataAniversario(LocalDate.of(2026, 5, 20));
        envio.setStatus(StatusEnvioAniversario.FALHA);
        envio.setErro("canal indisponível");

        when(envioRepository.findByIgrejaIdInAndDataAniversarioBetweenOrderByDataProcessamentoDesc(
                List.of(igrejaId), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
                .thenReturn(List.of(envio));

        List<AniversarioEnvio> lista = aniversarioService.listarEnvios(
                List.of(igrejaId), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getStatus()).isEqualTo(StatusEnvioAniversario.FALHA);
        assertThat(lista.get(0).getErro()).isEqualTo("canal indisponível");
    }

    @Test
    void naoDeveListarEnviosSemIgrejaVisivel() {
        assertThat(aniversarioService.listarEnvios(List.of(), LocalDate.now(), LocalDate.now())).isEmpty();
        verify(envioRepository, never())
                .findByIgrejaIdInAndDataAniversarioBetweenOrderByDataProcessamentoDesc(
                        any(), any(), any());
    }

    }