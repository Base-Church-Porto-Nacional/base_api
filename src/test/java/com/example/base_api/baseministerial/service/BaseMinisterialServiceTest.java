package com.example.base_api.baseministerial.service;

import com.example.base_api.baseministerial.dto.BaseMinisterialCreateRequest;
import com.example.base_api.baseministerial.dto.BaseMinisterialUpdateRequest;
import com.example.base_api.baseministerial.entity.BaseMinisterial;
import com.example.base_api.baseministerial.repository.BaseMinisterialRepository;
import com.example.base_api.common.exception.BusinessException;
import com.example.base_api.ministerio.repository.MinisterioRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import com.example.base_api.security.AutorizacaoService;
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
class BaseMinisterialServiceTest {

    @Mock
    private BaseMinisterialRepository baseMinisterialRepository;
    @Mock
    private MinisterioRepository ministerioRepository;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private AutorizacaoService autorizacaoService;

    @InjectMocks
    private BaseMinisterialService baseMinisterialService;

    private final UUID igrejaId = UUID.randomUUID();

    @Test
    void naoDeveCriarBaseDuplicadaQuandoNomeVemComEspacos() {
        when(baseMinisterialRepository.findByIgrejaIdAndNomeIgnoreCase(igrejaId, "Artes"))
                .thenReturn(Optional.of(base("Artes")));

        assertThatThrownBy(() -> baseMinisterialService.criar(new BaseMinisterialCreateRequest(
                "  Artes ", null, true, igrejaId, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe uma base ministerial com este nome");
        verify(baseMinisterialRepository, never()).save(any(BaseMinisterial.class));
    }

    @Test
    void naoDeveRenomearBaseParaNomeExistenteComEspacos() {
        BaseMinisterial base = base("Ensino");
        when(baseMinisterialRepository.findById(base.getId())).thenReturn(Optional.of(base));
        when(baseMinisterialRepository.existsByIgrejaIdAndNomeIgnoreCaseAndIdNot(igrejaId, "Artes", base.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> baseMinisterialService.atualizar(base.getId(), new BaseMinisterialUpdateRequest(
                " Artes  ", null, true, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe uma base ministerial com este nome");
        verify(baseMinisterialRepository, never()).save(any(BaseMinisterial.class));
    }

    private BaseMinisterial base(String nome) {
        BaseMinisterial base = new BaseMinisterial();
        base.setId(UUID.randomUUID());
        base.setIgrejaId(igrejaId);
        base.setNome(nome);
        return base;
    }
}
