package com.example.base_api.cronograma.repository;

import com.example.base_api.cronograma.entity.CronogramaConteudo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CronogramaConteudoRepository extends JpaRepository<CronogramaConteudo, UUID> {

    Optional<CronogramaConteudo> findByEventoId(UUID eventoId);

    /** Conteúdos de eventos da igreja com data >= hoje em que a pessoa é a pregadora. */
    @Query("""
            SELECT c FROM CronogramaConteudo c
            JOIN CronogramaEvento ev ON ev.id = c.eventoId
            WHERE ev.igrejaId = :igrejaId
              AND c.pregadorPessoaId = :pessoaId
              AND ev.dataEvento >= :hoje
            """)
    List<CronogramaConteudo> findFuturosComPregador(
            @Param("igrejaId") UUID igrejaId,
            @Param("pessoaId") UUID pessoaId,
            @Param("hoje") LocalDate hoje
    );
}
