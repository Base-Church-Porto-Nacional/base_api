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

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "small_group_encontro")
public class SmallGroupEncontro extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "small_group_id", nullable = false)
    private UUID smallGroupId;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "anfitriao_pessoa_id")
    private UUID anfitriaoPessoaId;

    @Column(columnDefinition = "TEXT")
    private String anotacoes;
}