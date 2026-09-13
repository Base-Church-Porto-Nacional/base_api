package com.example.base_api.cronograma.entity;

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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "cronograma_evento")
public class CronogramaEvento extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 300)
    private String tema;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEvento tipo = TipoEvento.CULTO;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "data_evento", nullable = false)
    private LocalDate dataEvento;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim")
    private LocalTime horaFim;

    @Column(name = "local_evento", length = 200)
    private String localEvento;

    @Column(name = "igreja_id", nullable = false)
    private UUID igrejaId;

    @Column(nullable = false, length = 7)
    private String competencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEvento status = StatusEvento.AGENDADO;

    @Column(name = "supervisor_pessoa_id")
    private UUID supervisorPessoaId;

    @Column(nullable = false)
    private Boolean ativo = true;
}
