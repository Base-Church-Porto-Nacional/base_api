package com.example.base_api.smallgroup.entity;

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
@Table(name = "small_group")
public class SmallGroup extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "igreja_id", nullable = false)
    private UUID igrejaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 20)
    private DiaSemana diaSemana = DiaSemana.DOMINGO;

    @Column(length = 10)
    private String horario;

    /** Único líder do grupo. */
    @Column(name = "lider_pessoa_id")
    private UUID liderPessoaId;

    /** Ministério de pequenos grupos ao qual o grupo pertence (tem líder/coordenador próprios). */
    @Column(name = "ministerio_id")
    private UUID ministerioId;

    @Column(nullable = false)
    private Boolean ativo = true;

    /** Nota do grupo (0 a 5). */
    @Column(nullable = false)
    private Integer nota = 0;
}