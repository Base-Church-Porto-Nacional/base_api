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
@Table(name = "pessoa_historico")
public class PessoaHistorico extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pessoa_id", nullable = false)
    private UUID pessoaId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "autor_nome", nullable = false, length = 150)
    private String autorNome;

    @Column(name = "autor_cargo", length = 100)
    private String autorCargo;

    @Column(name = "autor_pessoa_id")
    private UUID autorPessoaId;

    @Column(name = "data_registro", nullable = false)
    private LocalDate dataRegistro = LocalDate.now();
}
