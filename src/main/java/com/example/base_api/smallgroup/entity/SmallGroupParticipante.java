package com.example.base_api.smallgroup.entity;

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
@Table(name = "small_group_participante")
public class SmallGroupParticipante extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "encontro_id", nullable = false)
    private UUID encontroId;

    @Column(name = "pessoa_id")
    private UUID pessoaId;

    @Column(name = "pessoa_nome", nullable = false, length = 150)
    private String pessoaNome;
}