package com.example.base_api.aniversario.service;

import com.example.base_api.aniversario.config.MensagemAniversarioProperties;
import com.example.base_api.aniversario.dto.MensagemAniversarioRequest;
import com.example.base_api.aniversario.dto.MensagemAniversarioResponse;
import com.example.base_api.aniversario.entity.MensagemAniversario;
import com.example.base_api.aniversario.repository.MensagemAniversarioRepository;
import com.example.base_api.audit.service.AuditService;
import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.repository.IgrejaRepository;
import com.example.base_api.security.AutorizacaoService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MensagemAniversarioServiceTest {

    @Mock
    private MensagemAniversarioRepository repository;
    @Mock
    private IgrejaRepository igrejaRepository;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private AuditService auditService;

    private MensagemAniversarioProperties properties;
    private MensagemAniversarioService service;

    private final UUID matrizId = UUID.randomUUID();
    private final UUID filialId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        properties = new MensagemAniversarioProperties();
        properties.setTemplate("Padrão da configuração, {primeiroNome}!");
        service = new MensagemAniversarioService(
                repository, igrejaRepository, properties, autorizacaoService, auditService);
    }

    private MensagemAniversario mensagem(UUID igrejaId, String texto) {
        MensagemAniversario m = new MensagemAniversario();
        m.setId(UUID.randomUUID());
        m.setIgrejaId(igrejaId);
        m.setMensagem(texto);
        return m;
    }

    private void mockFilial() {
        Igreja filial = new Igreja();
        filial.setId(filialId);
        filial.setIgrejaMatrizId(matrizId);
        when(igrejaRepository.findById(filialId)).thenReturn(Optional.of(filial));
    }

    @Test
    void textoDeveSerOdaPropriaIgrejaQuandoCadastrado() {
        when(repository.findByIgrejaId(filialId)).thenReturn(Optional.of(mensagem(filialId, "Da filial")));

        assertThat(service.textoParaIgreja(filialId)).isEqualTo("Da filial");
    }

    @Test
    void filialSemMensagemDeveHerdarDaMatriz() {
        mockFilial();
        when(repository.findByIgrejaId(filialId)).thenReturn(Optional.empty());
        when(repository.findByIgrejaId(matrizId)).thenReturn(Optional.of(mensagem(matrizId, "Da matriz")));

        assertThat(service.textoParaIgreja(filialId)).isEqualTo("Da matriz");
    }

    @Test
    void semMensagemNaRedeDeveUsarTemplateDeConfiguracao() {
        mockFilial();

        assertThat(service.textoParaIgreja(filialId)).isEqualTo("Padrão da configuração, {primeiroNome}!");
    }

    @Test
    void obterSemCadastroDeveRetornarTextoHerdadoSemId() {
        when(autorizacaoService.igrejaAtualId()).thenReturn(filialId);
        mockFilial();

        MensagemAniversarioResponse resposta = service.obter(null);

        verify(autorizacaoService).assertAcesso(filialId);
        assertThat(resposta.id()).isNull();
        assertThat(resposta.igrejaId()).isEqualTo(filialId);
        assertThat(resposta.mensagem()).isEqualTo("Padrão da configuração, {primeiroNome}!");
    }

    @Test
    void salvarDeveCriarMensagemDaIgrejaEAuditar() {
        when(repository.save(any(MensagemAniversario.class))).thenAnswer(i -> i.getArgument(0));

        MensagemAniversarioResponse resposta = service.salvar(
                new MensagemAniversarioRequest(null, filialId, "  Feliz aniversário, [Nome]!  "));

        verify(autorizacaoService).assertPastorOuAdmin(filialId);
        assertThat(resposta.igrejaId()).isEqualTo(filialId);
        assertThat(resposta.mensagem()).isEqualTo("Feliz aniversário, [Nome]!");
        verify(auditService).registrar(any(), any(), any());
    }

    @Test
    void salvarDeveAtualizarMensagemExistente() {
        MensagemAniversario existente = mensagem(filialId, "Antiga");
        when(repository.findByIgrejaId(filialId)).thenReturn(Optional.of(existente));
        when(repository.save(any(MensagemAniversario.class))).thenAnswer(i -> i.getArgument(0));

        MensagemAniversarioResponse resposta = service.salvar(
                new MensagemAniversarioRequest(UUID.randomUUID(), filialId, "Nova"));

        ArgumentCaptor<MensagemAniversario> captor = ArgumentCaptor.forClass(MensagemAniversario.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existente);
        assertThat(resposta.id()).isEqualTo(existente.getId());
        assertThat(resposta.mensagem()).isEqualTo("Nova");
    }

    @Test
    void salvarSemSerPastorOuAdminDeveSerNegado() {
        doThrow(new AccessDeniedException("negado")).when(autorizacaoService).assertPastorOuAdmin(filialId);

        assertThatThrownBy(() -> service.salvar(new MensagemAniversarioRequest(null, filialId, "Oi")))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }
}
