package com.example.base_api.escala.entity;

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

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "escala_voluntario")
public class EscalaVoluntario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "escala_id", nullable = false)
    private UUID escalaId;

    @Column(name = "pessoa_id", nullable = false)
    private UUID pessoaId;

    @Column(name = "servico_id", nullable = false)
    private UUID servicoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusEscalaVoluntario status = StatusEscalaVoluntario.PENDENTE;

    @Column(columnDefinition = "TEXT")
    private String observacoes;
}
