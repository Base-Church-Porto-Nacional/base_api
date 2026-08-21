package com.example.base_api.voluntario.entity;

import com.example.base_api.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "pessoa_servico")
public class PessoaServico extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pessoa_id", nullable = false)
    private UUID pessoaId;

    @Column(name = "servico_id", nullable = false)
    private UUID servicoId;

    @Column(name = "data_entrada")
    private LocalDate dataEntrada;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "lider_responsavel_id")
    private UUID liderResponsavelId;
}
