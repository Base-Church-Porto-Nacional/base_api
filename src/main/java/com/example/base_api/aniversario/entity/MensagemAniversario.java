package com.example.base_api.aniversario.entity;

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
@Table(name = "mensagem_aniversario")
public class MensagemAniversario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "igreja_id", nullable = false, unique = true)
    private UUID igrejaId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String mensagem;
}
