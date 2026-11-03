package com.example.base_api.escala.repository;

import com.example.base_api.escala.entity.EscalaVoluntario;
import com.example.base_api.escala.entity.StatusEscalaVoluntario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EscalaVoluntarioRepository extends JpaRepository<EscalaVoluntario, UUID> {

    List<EscalaVoluntario> findByEscalaId(UUID escalaId);

    List<EscalaVoluntario> findByEscalaIdIn(List<UUID> escalaIds);

    @Query("""
            select new com.example.base_api.escala.repository.TotalPorEscala(ev.escalaId, count(ev))
            from EscalaVoluntario ev
            where ev.escalaId in :escalaIds
            group by ev.escalaId
            """)
    List<TotalPorEscala> contarPorEscala(@Param("escalaIds") Collection<UUID> escalaIds);

    List<EscalaVoluntario> findByPessoaIdInAndDataItemBetween(
            List<UUID> pessoaIds,
            LocalDate inicio,
            LocalDate fim
    );

    // Lista: com designações por data, a mesma pessoa pode ter várias linhas no mesmo serviço.
    List<EscalaVoluntario> findByEscalaIdAndPessoaIdAndServicoId(
            UUID escalaId,
            UUID pessoaId,
            UUID servicoId
    );

    Optional<EscalaVoluntario> findByEscalaIdAndPessoaIdAndServicoIdAndDataItem(
            UUID escalaId,
            UUID pessoaId,
            UUID servicoId,
            LocalDate dataItem
    );

    long countByEscalaIdAndServicoIdAndDataItemAndStatusIn(
            UUID escalaId,
            UUID servicoId,
            LocalDate dataItem,
            List<StatusEscalaVoluntario> status
    );

    long countByEscalaIdAndServicoIdAndStatusIn(
            UUID escalaId,
            UUID servicoId,
            List<StatusEscalaVoluntario> status
    );

    /**
     * Designações da pessoa ainda por acontecer nas escalas da igreja: data do item >= hoje, ou, sem data,
     * escala de competência >= mês atual.
     */
    @Query("""
            SELECT ev FROM EscalaVoluntario ev
            JOIN Escala e ON e.id = ev.escalaId
            WHERE e.igrejaId = :igrejaId
              AND ev.pessoaId = :pessoaId
              AND ((ev.dataItem IS NOT NULL AND ev.dataItem >= :hoje)
                   OR (ev.dataItem IS NULL AND e.competencia >= :competencia))
            """)
    List<EscalaVoluntario> findFuturasDaPessoaNaIgreja(
            @Param("pessoaId") UUID pessoaId,
            @Param("igrejaId") UUID igrejaId,
            @Param("hoje") LocalDate hoje,
            @Param("competencia") String competencia
    );
}
