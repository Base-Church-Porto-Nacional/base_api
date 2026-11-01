package com.example.base_api.escala.service;

import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.EscalaServico;
import com.example.base_api.escala.repository.EscalaServicoRepository;
import com.example.base_api.servico.entity.Servico;
import com.example.base_api.voluntario.entity.PessoaServico;
import com.example.base_api.voluntario.repository.PessoaServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NecessidadeEscalaServiceTest {

    @Mock
    private PessoaServicoRepository pessoaServicoRepository;
    @Mock
    private EscalaServicoRepository escalaServicoRepository;

    @InjectMocks
    private NecessidadeEscalaService necessidadeEscalaService;

    @Test
    void elegiveisSaoAtivosNaoFicticiosComVoluntarioConsultadosDeUmaVez() {
        Servico comVoluntario = servico(true, false);
        Servico semVoluntario = servico(true, false);
        Servico inativo = servico(false, false);
        Servico ficticio = servico(true, true);
        PessoaServico vinculo = new PessoaServico();
        vinculo.setServicoId(comVoluntario.getId());
        when(pessoaServicoRepository.findByServicoIdInAndAtivoTrue(List.of(comVoluntario.getId(), semVoluntario.getId())))
                .thenReturn(List.of(vinculo));

        List<UUID> elegiveis = necessidadeEscalaService.servicosElegiveis(
                List.of(comVoluntario, semVoluntario, inativo, ficticio));

        assertThat(elegiveis).containsExactly(comVoluntario.getId());
        verify(pessoaServicoRepository, times(1)).findByServicoIdInAndAtivoTrue(any());
    }

    @Test
    void semCandidatosReaisNaoConsultaVoluntarios() {
        assertThat(necessidadeEscalaService.servicosElegiveis(List.of(servico(false, false), servico(true, true))))
                .isEmpty();
        verify(pessoaServicoRepository, never()).findByServicoIdInAndAtivoTrue(any());
    }

    @Test
    void criaUmaNecessidadeDeUmaPessoaPorServico() {
        Escala escala = new Escala();
        escala.setId(UUID.randomUUID());
        UUID violao = UUID.randomUUID();
        UUID vocal = UUID.randomUUID();

        necessidadeEscalaService.criarNecessidades(escala, List.of(violao, vocal));

        ArgumentCaptor<EscalaServico> captor = ArgumentCaptor.forClass(EscalaServico.class);
        verify(escalaServicoRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(EscalaServico::getServicoId).containsExactly(violao, vocal);
        assertThat(captor.getAllValues()).allSatisfy(n -> {
            assertThat(n.getEscalaId()).isEqualTo(escala.getId());
            assertThat(n.getQuantidadeNecessaria()).isEqualTo(1);
        });
    }

    private Servico servico(boolean ativo, boolean ficcao) {
        Servico servico = new Servico();
        servico.setId(UUID.randomUUID());
        servico.setAtivo(ativo);
        servico.setFiccao(ficcao);
        return servico;
    }
}
