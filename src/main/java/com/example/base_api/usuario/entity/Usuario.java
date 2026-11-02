package com.example.base_api.usuario.entity;

import com.example.base_api.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "usuario")
public class Usuario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pessoa_id", nullable = false, unique = true)
    private UUID pessoaId;

    @Column(name = "igreja_id")
    private UUID igrejaId;

    @Column(unique = true, length = 255)
    private String email;

    /** Telefone normalizado (DDI 55 + DDD + número, só dígitos). */
    @Column(unique = true, length = 30)
    private String telefone;

    @Column(name = "senha_hash")
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusUsuario status = StatusUsuario.ATIVO;

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 0;

    @OneToMany(mappedBy = "usuario", fetch = FetchType.EAGER)
    private Set<UsuarioPapel> papeisIgreja = new HashSet<>();

    public boolean isAtivo() {
        return status == StatusUsuario.ATIVO;
    }

    public void invalidarTokens() {
        int atual = tokenVersion == null ? 0 : tokenVersion;
        this.tokenVersion = atual + 1;
    }

    public Set<String> nomesPapeis(UUID igrejaAlvoId) {
        if (papeisIgreja == null || igrejaAlvoId == null) {
            return Set.of();
        }
        return papeisIgreja.stream()
                .filter(up -> igrejaAlvoId.equals(up.getIgrejaId()) && up.getPapel() != null)
                .map(up -> up.getPapel().getNome())
                .collect(Collectors.toSet());
    }

    /** Só papéis de sistema (globais); papéis customizados de mesmo nome não contam. */
    public Set<String> nomesPapeisSistema(UUID igrejaAlvoId) {
        if (papeisIgreja == null || igrejaAlvoId == null) {
            return Set.of();
        }
        return papeisIgreja.stream()
                .filter(up -> igrejaAlvoId.equals(up.getIgrejaId()) && up.getPapel() != null && up.getPapel().isSistema())
                .map(up -> up.getPapel().getNome())
                .collect(Collectors.toSet());
    }

    /** Possui, nesta igreja, um papel customizado que permite lançar a contagem do culto. */
    public boolean temPapelComContagem(UUID igrejaAlvoId) {
        if (papeisIgreja == null || igrejaAlvoId == null) {
            return false;
        }
        return papeisIgreja.stream()
                .anyMatch(up -> igrejaAlvoId.equals(up.getIgrejaId()) && up.getPapel() != null
                        && !up.getPapel().isSistema() && igrejaAlvoId.equals(up.getPapel().getIgrejaId())
                        && up.getPapel().isPodeLancarContagem());
    }

    public Set<String> nomesPapeisDaIgrejaAtual() {
        return nomesPapeis(igrejaId);
    }
}
