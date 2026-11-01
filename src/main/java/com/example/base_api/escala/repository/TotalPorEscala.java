package com.example.base_api.escala.repository;

import java.util.UUID;

/** Projeção: quantidade de designações de uma escala. */
public record TotalPorEscala(UUID escalaId, Long total) {
}
