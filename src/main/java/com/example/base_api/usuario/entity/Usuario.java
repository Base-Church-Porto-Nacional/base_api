package com.example.base_api.usuario.entity;

import com.example.base_api.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "pessoa_id", unique = true)
    private UUID pessoaId;

    @Column(name = "igreja_id")
    private UUID igrejaId;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 0;

    @OneToMany(mappedBy = "usuario", fetch = FetchType.EAGER)
    private Set<UsuarioPapel> papeisIgreja = new HashSet<>();

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

    public Set<String> nomesPapeisDaIgrejaAtual() {
        return nomesPapeis(igrejaId);
    }
}
