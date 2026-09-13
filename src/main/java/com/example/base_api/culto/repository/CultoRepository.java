package com.example.base_api.culto.repository;

import com.example.base_api.culto.entity.Culto;
import com.example.base_api.culto.entity.StatusCulto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CultoRepository extends JpaRepository<Culto, UUID> {

    @Query("""
            SELECT c FROM Culto c
            WHERE c.igrejaId IN :igrejaIds
              AND (:categoriaId IS NULL OR c.categoriaCultoId = :categoriaId)
              AND (:competencia IS NULL OR c.competencia = :competencia)
              AND (:status IS NULL OR c.status = :status)
              AND (:inicio IS NULL OR c.data >= :inicio)
              AND (:fim IS NULL OR c.data <= :fim)
              AND (:ativo IS NULL OR c.ativo = :ativo)
            ORDER BY c.data ASC, c.horaInicio ASC
            """)
    List<Culto> filtrar(
            @Param("igrejaIds") List<UUID> igrejaIds,
            @Param("categoriaId") UUID categoriaId,
            @Param("competencia") String competencia,
            @Param("status") StatusCulto status,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim,
            @Param("ativo") Boolean ativo
    );

    Optional<Culto> findByCategoriaCultoIdAndData(UUID categoriaCultoId, LocalDate data);

    boolean existsByCategoriaCultoIdAndData(UUID categoriaCultoId, LocalDate data);

    List<Culto> findByCategoriaCultoIdAndIgrejaIdInOrderByDataAsc(UUID categoriaCultoId, List<UUID> igrejaIds);

    long countByCategoriaCultoIdAndAtivoTrue(UUID categoriaCultoId);
}