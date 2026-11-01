package com.example.base_api.usuario.service;

import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.usuario.dto.PapelRequest;
import com.example.base_api.usuario.dto.PapelResponse;
import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.repository.PapelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PapelServiceTest {

    @Mock
    private PapelRepository papelRepository;
    @Mock
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private PapelService papelService;

    @Test
    void naoExcluiPapelSistema() {
        UUID id = UUID.randomUUID();
        Papel admin = new Papel();
        admin.setId(id);
        admin.setNome("ADMIN");
        // igrejaId nulo => papel de sistema (global)

        when(papelRepository.findById(id)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> papelService.excluir(id))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("sistema");
        verify(papelRepository, never()).delete(admin);
    }

    @Test
    void naoAlteraPapelSistema() {
        UUID id = UUID.randomUUID();
        Papel pastor = new Papel();
        pastor.setId(id);
        pastor.setNome("PASTOR");

        when(papelRepository.findById(id)).thenReturn(Optional.of(pastor));

        assertThatThrownBy(() -> papelService.atualizar(id, new PapelRequest("LOUVOR", "novo", null, null)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("sistema");
        verify(papelRepository, never()).save(pastor);
    }

    @Test
    void deveCriarPapelCustomizadoComPermissaoDeContagem() {
        UUID igrejaId = UUID.randomUUID();
        when(autorizacaoService.igrejaAtualId()).thenReturn(igrejaId);
        when(papelRepository.save(org.mockito.ArgumentMatchers.any(Papel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PapelResponse response = papelService.criar(new PapelRequest(" contador ", "Conta o culto", true, null));

        assertThat(response.nome()).isEqualTo("CONTADOR");
        assertThat(response.podeLancarContagem()).isTrue();
        assertThat(response.igrejaId()).isEqualTo(igrejaId);
    }

    @Test
    void permissaoDeContagemAusenteViraFalse() {
        UUID igrejaId = UUID.randomUUID();
        when(autorizacaoService.igrejaAtualId()).thenReturn(igrejaId);
        when(papelRepository.save(org.mockito.ArgumentMatchers.any(Papel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PapelResponse response = papelService.criar(new PapelRequest("RECEPCAO", null, null, null));

        assertThat(response.podeLancarContagem()).isFalse();
    }

    @Test
    void matrizCriaPapelNaIgrejaInformada() {
        UUID filialId = UUID.randomUUID();
        when(papelRepository.save(org.mockito.ArgumentMatchers.any(Papel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PapelResponse response = papelService.criar(new PapelRequest("CONTADOR", null, true, filialId));

        assertThat(response.igrejaId()).isEqualTo(filialId);
        verify(autorizacaoService).assertPastorOuAdmin(filialId);
        verify(autorizacaoService, never()).igrejaAtualId();
    }

    @Test
    void naoCriaPapelDuplicadoNaMesmaIgreja() {
        UUID igrejaId = UUID.randomUUID();
        when(papelRepository.existsByNomeAndIgrejaId("CONTADOR", igrejaId)).thenReturn(true);

        assertThatThrownBy(() -> papelService.criar(new PapelRequest("contador", null, true, igrejaId)))
                .isInstanceOf(com.example.base_api.common.exception.ConflictException.class);
        verify(papelRepository, never()).save(org.mockito.ArgumentMatchers.any(Papel.class));
    }

    @Test
    void listarDeOutraIgrejaVerificaAcesso() {
        UUID filialId = UUID.randomUUID();
        when(papelRepository.findByIgrejaIdIsNullOrIgrejaId(filialId)).thenReturn(java.util.List.of());

        papelService.listar(filialId);

        verify(autorizacaoService).assertAcesso(filialId);
        verify(papelRepository).findByIgrejaIdIsNullOrIgrejaId(filialId);
    }
}
