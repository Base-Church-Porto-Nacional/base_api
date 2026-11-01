package com.example.base_api.smallgroup.service;

import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
import com.example.base_api.smallgroup.dto.SmallGroupCreateRequest;
import com.example.base_api.smallgroup.dto.SmallGroupUpdateRequest;
import com.example.base_api.smallgroup.entity.DiaSemana;
import com.example.base_api.smallgroup.entity.SmallGroup;
import com.example.base_api.smallgroup.repository.SmallGroupEncontroRepository;
import com.example.base_api.smallgroup.repository.SmallGroupParticipanteRepository;
import com.example.base_api.smallgroup.repository.SmallGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmallGroupServiceTest {

    @Mock
    private SmallGroupRepository smallGroupRepository;
    @Mock
    private SmallGroupEncontroRepository encontroRepository;
    @Mock
    private SmallGroupParticipanteRepository participanteRepository;
    @Mock
    private MinisterioRepository ministerioRepository;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private SmallGroupService smallGroupService;

    private final UUID igrejaId = UUID.randomUUID();
    private final UUID liderId = UUID.randomUUID();

    @Test
    void naoDeveCriarGrupoDuplicadoQuandoNomeVemComEspacos() {
        SmallGroup existente = grupo("Jovens");
        when(smallGroupRepository.findByIgrejaIdAndNomeIgnoreCase(igrejaId, "Jovens"))
                .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> smallGroupService.criar(new SmallGroupCreateRequest(
                "  Jovens ", igrejaId, DiaSemana.QUARTA, null, liderId, null, true, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um pequeno grupo com este nome");
        verify(smallGroupRepository, never()).save(any(SmallGroup.class));
    }

    @Test
    void naoDeveRenomearGrupoParaNomeExistenteComEspacos() {
        SmallGroup grupo = grupo("Casais");
        when(smallGroupRepository.findById(grupo.getId())).thenReturn(Optional.of(grupo));
        when(smallGroupRepository.existsByIgrejaIdAndNomeIgnoreCaseAndIdNot(igrejaId, "Jovens", grupo.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> smallGroupService.atualizar(grupo.getId(), new SmallGroupUpdateRequest(
                " Jovens  ", DiaSemana.QUARTA, null, liderId, null, true, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um pequeno grupo com este nome");
        verify(smallGroupRepository, never()).save(any(SmallGroup.class));
    }

    private SmallGroup grupo(String nome) {
        SmallGroup grupo = new SmallGroup();
        grupo.setId(UUID.randomUUID());
        grupo.setIgrejaId(igrejaId);
        grupo.setNome(nome);
        return grupo;
    }
}
