package com.example.base_api.escala.entity;

import com.example.base_api.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "escala_servico")
public class EscalaServico extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "escala_id", nullable = false)
    private UUID escalaId;

    @Column(name = "servico_id", nullable = false)
    private UUID servicoId;

    @Column(name = "quantidade_necessaria", nullable = false)
    private Integer quantidadeNecessaria = 1;
}
