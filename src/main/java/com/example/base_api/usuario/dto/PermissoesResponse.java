package com.example.base_api.usuario.dto;

import com.example.base_api.security.Permissoes;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/** Permissões efetivas do usuário, calculadas no servidor, para o app decidir o que exibir. */
public record PermissoesResponse(
        List<String> capacidades,
        List<UUID> ministeriosLiderados,
        List<UUID> basesLideradas,
        List<UUID> gruposLiderados,
        boolean ehVoluntario
) {

    public static PermissoesResponse de(Permissoes p) {
        return new PermissoesResponse(
                ordenar(p.capacidades().stream().map(Enum::name)),
                ordenar(p.ministeriosLiderados().stream()),
                ordenar(p.basesLideradas().stream()),
                ordenar(p.gruposLiderados().stream()),
                p.ehVoluntario());
    }

    private static <T> List<T> ordenar(Stream<T> itens) {
        return itens.sorted(Comparator.comparing(Object::toString)).toList();
    }
}
