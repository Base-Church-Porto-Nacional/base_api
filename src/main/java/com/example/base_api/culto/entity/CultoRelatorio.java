package com.example.base_api.culto.entity;

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
@Table(name = "culto_relatorio")
public class CultoRelatorio extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "culto_id", nullable = false, unique = true)
    private UUID cultoId;

    public static CultoRelatorio vazio(UUID cultoId) {
        CultoRelatorio relatorio = new CultoRelatorio();
        relatorio.setCultoId(cultoId);
        return relatorio;
    }

    @Column(nullable = false)
    private Integer adultos = 0;

    @Column(nullable = false)
    private Integer criancas = 0;

    @Column(nullable = false)
    private Integer conversoes = 0;

    @Column(nullable = false)
    private Integer reconciliacoes = 0;

    @Column(nullable = false)
    private Integer aliancas = 0;

    @Column(nullable = false)
    private Integer carros = 0;

    @Column(nullable = false)
    private Integer motos = 0;

    @Column(nullable = false)
    private Integer bicicletas = 0;

    @Column(columnDefinition = "TEXT")
    private String observacoes;
}
