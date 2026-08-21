package com.example.base_api.jornada.repository;

import com.example.base_api.jornada.entity.JornadaEtapaCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JornadaEtapaCatalogoRepository extends JpaRepository<JornadaEtapaCatalogo, UUID> {

    Optional<JornadaEtapaCatalogo> findByCodigo(String codigo);

    List<JornadaEtapaCatalogo> findByAtivoTrueOrderByOrdemAsc();
}
