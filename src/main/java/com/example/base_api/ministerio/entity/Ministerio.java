package com.example.base_api.ministerio.entity;

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
@Table(name = "ministerio")
public class Ministerio extends AuditableEntity {

    public static final String SERVICO_COORDENADOR = "Coordenador";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "igreja_id", nullable = false)
    private UUID igrejaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MinisterioTipo tipo = MinisterioTipo.GERAL;

    @Column(name = "lider_pessoa_id")
    private UUID liderPessoaId;

    @Column(name = "coordenador_pessoa_id")
    private UUID coordenadorPessoaId;

    @Column(columnDefinition = "TEXT")
    private String anotacoes;

    @Column(columnDefinition = "TEXT")
    private String ordem;

    @Column(nullable = false)
    private Boolean ativo = true;
}
