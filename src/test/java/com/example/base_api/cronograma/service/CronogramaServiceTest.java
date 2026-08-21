package com.example.base_api.cronograma.service;

import com.example.base_api.cronograma.dto.CronogramaEventoCreateRequest;
import com.example.base_api.cronograma.dto.CronogramaEventoResponse;
import com.example.base_api.cronograma.entity.CronogramaEvento;
import com.example.base_api.cronograma.entity.TipoEvento;
import com.example.base_api.cronograma.repository.CronogramaEventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CronogramaServiceTest {

    @Mock
    private CronogramaEventoRepository repository;

    @InjectMocks
    private CronogramaService cronogramaService;

    @Test
    void deveCriarEventoNoCronograma() {
        when(repository.save(any(CronogramaEvento.class))).thenAnswer(invocation -> {
            CronogramaEvento e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });

        CronogramaEventoResponse response = cronogramaService.criar(new CronogramaEventoCreateRequest(
                "Culto de Domingo",
                TipoEvento.CULTO,
                null,
                LocalDate.of(2026, 8, 24),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                "Templo"
        ));

        assertThat(response.titulo()).isEqualTo("Culto de Domingo");
        assertThat(response.tipo()).isEqualTo(TipoEvento.CULTO);
        assertThat(response.ativo()).isTrue();
    }
}
