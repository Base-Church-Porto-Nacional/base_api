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
@Table(name = "jornada_membro")
public class JornadaMembro extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pessoa_id", nullable = false, unique = true)
    private UUID pessoaId;

    @Column(name = "aceitou_jesus", nullable = false)
    private Boolean aceitouJesus = false;

    @Column(name = "data_aceitou_jesus")
    private LocalDate dataAceitouJesus;

    @Column(nullable = false)
    private Boolean batizado = false;

    @Column(name = "data_batismo")
    private LocalDate dataBatismo;

    @Column(name = "concluiu_trilha_base", nullable = false)
    private Boolean concluiuTrilhaBase = false;

    @Column(name = "data_conclusao_trilha_base")
    private LocalDate dataConclusaoTrilhaBase;

    @Column(nullable = false)
    private Boolean membro = false;

    @Column(name = "data_membro")
    private LocalDate dataMembro;
}
