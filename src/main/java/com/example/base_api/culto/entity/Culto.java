package com.example.base_api.culto.entity;

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
@Table(name = "culto")
public class Culto extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "igreja_id", nullable = false)
    private UUID igrejaId;

    @Column(name = "categoria_culto_id", nullable = false)
    private UUID categoriaCultoId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 300)
    private String tema;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim")
    private LocalTime horaFim;

    @Column(name = "pregador_id")
    private UUID pregadorId;

    @Column(columnDefinition = "TEXT")
    private String louvor;

    @Column(name = "texto_pregacao", columnDefinition = "TEXT")
    private String textoPregacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCulto status = StatusCulto.AGENDADO;

    @Column(name = "escala_id")
    private UUID escalaId;

    @Column(nullable = false, length = 7)
    private String competencia;

    @Column(nullable = false)
    private Boolean ativo = true;
}
