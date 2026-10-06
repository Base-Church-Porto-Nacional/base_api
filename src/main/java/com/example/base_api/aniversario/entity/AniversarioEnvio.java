package com.example.base_api.aniversario.entity;

import com.example.base_api.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "aniversario_envio")
public class AniversarioEnvio extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pessoa_id", nullable = false)
    private UUID pessoaId;

    @Column(name = "igreja_id", nullable = false)
    private UUID igrejaId;

    @Column(name = "data_aniversario", nullable = false)
    private LocalDate dataAniversario;

    @Column(name = "data_processamento", nullable = false)
    private Instant dataProcessamento = Instant.now();

    @Column(columnDefinition = "TEXT", nullable = false)
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEnvioAniversario status = StatusEnvioAniversario.PENDENTE;

    @Column(columnDefinition = "TEXT")
    private String erro;
}