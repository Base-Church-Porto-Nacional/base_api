package com.example.base_api.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "usuario_papel")
@IdClass(UsuarioPapelId.class)
public class UsuarioPapel {

    @Id
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Id
    @Column(name = "papel_id", nullable = false)
    private UUID papelId;

    @Id
    @Column(name = "igreja_id", nullable = false)
    private UUID igrejaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", insertable = false, updatable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "papel_id", insertable = false, updatable = false)
    private Papel papel;
}
