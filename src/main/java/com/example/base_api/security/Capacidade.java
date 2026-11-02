package com.example.base_api.security;

import java.util.EnumSet;
import java.util.Set;

/** Capacidades concedidas por papel. A única fonte no servidor; o cliente apenas as consome. */
public enum Capacidade {
    PESSOAS_VER_TODAS,
    PESSOAS_GERIR,
    DADOS_PASTORAIS_EDITAR,
    CONTAS_GERIR,
    MINISTERIOS_GERIR,
    ESCALAS_GERIR,
    CULTO_CONTEUDO_EDITAR,
    CULTO_CONTAGEM,
    PAPEIS_GESTAO_ATRIBUIR,
    PAPEIS_CUSTOM_GERIR,
    EXPORTAR,
    CONFIGURAR,
    RELATORIOS_VER,
    FILIAL_CRIAR;

    /** Exclusivas do pastor. */
    private static final Set<Capacidade> SO_PASTOR =
            EnumSet.of(CULTO_CONTEUDO_EDITAR, PAPEIS_GESTAO_ATRIBUIR, FILIAL_CRIAR);

    /**
     * @param papeis nomes de papéis de SISTEMA (já com cascata da matriz); nomes customizados nunca devem ser passados aqui
     * @param papelComContagem usuário tem papel customizado que permite lançar a contagem
     */
    public static Set<Capacidade> de(Set<String> papeis, boolean papelComContagem) {
        EnumSet<Capacidade> resultado = EnumSet.noneOf(Capacidade.class);
        if (papeis.contains(Papeis.PASTOR)) {
            resultado.addAll(EnumSet.allOf(Capacidade.class));
        } else if (papeis.contains(Papeis.SECRETARIA)) {
            resultado.addAll(EnumSet.complementOf(EnumSet.copyOf(SO_PASTOR)));
        }
        if (papelComContagem) {
            resultado.add(CULTO_CONTAGEM);
        }
        return Set.copyOf(resultado);
    }
}
