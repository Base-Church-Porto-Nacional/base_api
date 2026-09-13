package com.example.base_api.baseministerial.entity;

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
@Table(name = "base_ministerial")
public class BaseMinisterial extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "igreja_id", nullable = false)
    private UUID igrejaId;

    @Column(name = "lider_pessoa_id")
    private UUID liderPessoaId;

    @Column(name = "coordenador_pessoa_id")
    private UUID coordenadorPessoaId;

    @Column(columnDefinition = "TEXT")
    private String ordem;

    @Column(nullable = false)
    private Boolean ativo = true;
}