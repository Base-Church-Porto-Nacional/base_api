package com.example.base_api.pessoa.entity;

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
@Table(name = "pessoa")
public class Pessoa extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "nome_social", length = 150)
    private String nomeSocial;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String telefone;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "data_cadastro", nullable = false)
    private LocalDate dataCadastro = LocalDate.now();

    @Column(name = "igreja_id", nullable = false)
    private UUID igrejaId;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(columnDefinition = "TEXT")
    private String observacoes;
}
