package com.example.base_api.usuario.entity;

/** Ciclo de vida da conta: convidada (sem senha definida), ativa ou desativada. Só ATIVO entra. */
public enum StatusUsuario {
    CONVIDADO,
    ATIVO,
    DESATIVADO
}
