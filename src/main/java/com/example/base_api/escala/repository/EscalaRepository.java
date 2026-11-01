package com.example.base_api.escala.repository;

import com.example.base_api.escala.entity.Escala;
import com.example.base_api.escala.entity.TipoEscala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EscalaRepository extends JpaRepository<Escala, UUID> {

    List<Escala> findByIgrejaIdInOrderByCompetenciaDescTituloAsc(List<UUID> igrejaIds);

    List<Escala> findByIgrejaIdInAndCompetenciaOrderByTipoAscTituloAsc(List<UUID> igrejaIds, String competencia);

    Optional<Escala> findFirstByIgrejaIdAndCompetenciaAndTipo(
            UUID igrejaId,
            String competencia,
            TipoEscala tipo
    );

    Optional<Escala> findFirstByIgrejaIdAndCompetenciaAndTipoAndMinisterioId(
            UUID igrejaId,
            String competencia,
            TipoEscala tipo,
            UUID ministerioId
    );

    List<Escala> findByIgrejaIdAndCompetenciaAndTipo(
            UUID igrejaId,
            String competencia,
            TipoEscala tipo
    );

    List<Escala> findByEventoId(UUID eventoId);

    List<Escala> findByEventoIdIn(Collection<UUID> eventoIds);

    Optional<Escala> findByEventoIdAndMinisterioId(UUID eventoId, UUID ministerioId);

    List<Escala> findByMinisterioIdAndCompetenciaAndAtivoTrueOrderByDataEventoAsc(
            UUID ministerioId,
            String competencia
    );

    long countByIgrejaIdIn(List<UUID> igrejaIds);

    @Query("""
            SELECT DISTINCT e FROM Escala e
            JOIN EscalaVoluntario ev ON ev.escalaId = e.id
            WHERE e.igrejaId IN :igrejaIds
              AND ev.pessoaId = :pessoaId
            ORDER BY e.competencia DESC, e.titulo
            """)
    List<Escala> findByIgrejaIdInAndPessoaDesignada(
            @Param("igrejaIds") List<UUID> igrejaIds,
            @Param("pessoaId") UUID pessoaId
    );
}
