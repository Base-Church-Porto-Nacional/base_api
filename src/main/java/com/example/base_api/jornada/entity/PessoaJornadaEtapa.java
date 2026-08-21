package com.example.base_api.jornada.entity;

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
@Table(name = "pessoa_jornada_etapa")
public class PessoaJornadaEtapa extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pessoa_id", nullable = false)
    private UUID pessoaId;

    @Column(name = "etapa_id", nullable = false)
    private UUID etapaId;

    @Column(nullable = false)
    private Boolean concluida = false;

    @Column(name = "data_conclusao")
    private LocalDate dataConclusao;

    @Column(columnDefinition = "TEXT")
    private String observacoes;
}
