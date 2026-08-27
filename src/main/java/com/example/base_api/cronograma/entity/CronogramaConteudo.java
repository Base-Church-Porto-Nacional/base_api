package com.example.base_api.cronograma.entity;

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
@Table(name = "cronograma_conteudo")
public class CronogramaConteudo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "evento_id", nullable = false, unique = true)
    private UUID eventoId;

    @Column(name = "texto_pregacao", columnDefinition = "TEXT")
    private String textoPregacao;

    @Column(columnDefinition = "TEXT")
    private String louvor;

    @Column(name = "pregador_pessoa_id")
    private UUID pregadorPessoaId;
}
