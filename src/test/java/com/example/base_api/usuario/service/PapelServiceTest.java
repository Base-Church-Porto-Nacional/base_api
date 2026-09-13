package com.example.base_api.usuario.service;

import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.usuario.dto.PapelRequest;
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

        assertThatThrownBy(() -> papelService.atualizar(id, new PapelRequest("LOUVOR", "novo")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("sistema");
        verify(papelRepository, never()).save(pastor);
    }
}
