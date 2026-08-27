package com.example.base_api.igreja.entity;

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
@Table(name = "igreja")
public class Igreja extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IgrejaTipo tipo = IgrejaTipo.FILIAL;

    @Column(name = "igreja_matriz_id")
    private UUID igrejaMatrizId;

    @Column(name = "pastor_pessoa_id")
    private UUID pastorPessoaId;

    @Column(name = "admin_usuario_id")
    private UUID adminUsuarioId;

    @Column(nullable = false)
    private Boolean ativo = true;

    public boolean isMatriz() {
        return tipo == IgrejaTipo.MATRIZ;
    }
}
