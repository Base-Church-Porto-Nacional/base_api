package com.example.base_api.pessoa.service;

import com.example.base_api.pessoa.dto.PessoaHistoricoCreateRequest;
import com.example.base_api.pessoa.dto.PessoaHistoricoResponse;
import com.example.base_api.pessoa.entity.PessoaHistorico;
import com.example.base_api.pessoa.repository.PessoaHistoricoRepository;
import com.example.base_api.pessoa.repository.PessoaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PessoaHistoricoServiceTest {

    @Mock
    private PessoaHistoricoRepository historicoRepository;

    @Mock
    private PessoaRepository pessoaRepository;

    @InjectMocks
    private PessoaHistoricoService pessoaHistoricoService;

    @Test
    void deveCriarRegistroDeHistorico() {
        UUID pessoaId = UUID.randomUUID();
        when(pessoaRepository.existsById(pessoaId)).thenReturn(true);
        when(historicoRepository.save(any(PessoaHistorico.class))).thenAnswer(invocation -> {
            PessoaHistorico h = invocation.getArgument(0);
            h.setId(UUID.randomUUID());
            h.setCreatedAt(Instant.now());
            h.setUpdatedAt(Instant.now());
            return h;
        });

        PessoaHistoricoResponse response = pessoaHistoricoService.criar(
                pessoaId,
                new PessoaHistoricoCreateRequest(
                        "Voluntário pediu para ficar no banco por 15 dias devido a problema na coluna",
                        "Carla",
                        "Pastora",
                        null,
                        LocalDate.of(2026, 8, 20)
                )
        );

        assertThat(response.descricao()).contains("ficar no banco");
        assertThat(response.autorNome()).isEqualTo("Carla");
        assertThat(response.autorCargo()).isEqualTo("Pastora");
        assertThat(response.dataRegistro()).isEqualTo(LocalDate.of(2026, 8, 20));
    }
}
