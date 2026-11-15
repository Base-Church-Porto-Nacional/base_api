package com.example.base_api.security;

import java.util.Set;
import java.util.UUID;

/** Permissões do usuário numa igreja: capacidades por papel e escopos derivados (funções por vínculo). */
public record Permissoes(
        UUID igrejaId,
        Set<String> papeis,
        Set<Capacidade> capacidades,
        Set<UUID> ministeriosLiderados,
        Set<UUID> basesLideradas,
        Set<UUID> gruposLiderados,
        boolean ehVoluntario) {

    public Permissoes {
        papeis = Set.copyOf(papeis);
        capacidades = Set.copyOf(capacidades);
        ministeriosLiderados = Set.copyOf(ministeriosLiderados);
        basesLideradas = Set.copyOf(basesLideradas);
        gruposLiderados = Set.copyOf(gruposLiderados);
    }

    public boolean tem(Capacidade capacidade) {
        return capacidades.contains(capacidade);
    }
}
