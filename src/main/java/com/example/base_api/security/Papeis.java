package com.example.base_api.security;

import java.util.Set;

/** Nomes dos papéis de sistema e dos nomes reservados (não podem ser papel customizado). */
public final class Papeis {

    public static final String PASTOR = "PASTOR";
    public static final String SECRETARIA = "SECRETARIA";

    /** Papéis de gestão: cascateiam da matriz para as filiais. */
    public static final Set<String> GESTAO = Set.of(PASTOR, SECRETARIA);

    public static final Set<String> RESERVADOS = Set.of(
            "ADMIN", "LIDER", "VOLUNTARIO", "MEMBRO",
            "COORDENADOR", "SUPERVISOR", "PREGADOR",
            PASTOR, SECRETARIA
    );

    private Papeis() {
    }
}
