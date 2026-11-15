package com.example.base_api.baseministerial.repository;

import com.example.base_api.baseministerial.entity.BaseMinisterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BaseMinisterialRepository extends JpaRepository<BaseMinisterial, UUID> {

    Optional<BaseMinisterial> findByIgrejaIdAndNomeIgnoreCase(UUID igrejaId, String nome);

    boolean existsByIgrejaIdAndNomeIgnoreCaseAndIdNot(UUID igrejaId, String nome, UUID id);

    List<BaseMinisterial> findByIgrejaIdInOrderByNomeAsc(List<UUID> igrejaIds);

    @org.springframework.data.jpa.repository.Query("""
            select b from BaseMinisterial b
            where b.igrejaId = :igrejaId and (b.liderPessoaId = :pessoaId or b.coordenadorPessoaId = :pessoaId)
            """)
    List<BaseMinisterial> findLideradasOuCoordenadas(@org.springframework.data.repository.query.Param("igrejaId") UUID igrejaId,
                                                     @org.springframework.data.repository.query.Param("pessoaId") UUID pessoaId);

    /** Escopo de liderança: só bases ativas (inativa não faz ninguém líder). */
    @org.springframework.data.jpa.repository.Query("""
            select b from BaseMinisterial b
            where b.igrejaId = :igrejaId and b.ativo = true
              and (b.liderPessoaId = :pessoaId or b.coordenadorPessoaId = :pessoaId)
            """)
    List<BaseMinisterial> findAtivasLideradasOuCoordenadas(@org.springframework.data.repository.query.Param("igrejaId") UUID igrejaId,
                                                           @org.springframework.data.repository.query.Param("pessoaId") UUID pessoaId);

    @org.springframework.data.jpa.repository.Query("select distinct b.liderPessoaId from BaseMinisterial b where b.igrejaId in :igrejaIds and b.ativo = true and b.liderPessoaId is not null")
    List<UUID> idsLideresDeAtivas(@org.springframework.data.repository.query.Param("igrejaIds") java.util.Collection<UUID> igrejaIds);

    @org.springframework.data.jpa.repository.Query("select distinct b.coordenadorPessoaId from BaseMinisterial b where b.igrejaId in :igrejaIds and b.ativo = true and b.coordenadorPessoaId is not null")
    List<UUID> idsCoordenadoresDeAtivas(@org.springframework.data.repository.query.Param("igrejaIds") java.util.Collection<UUID> igrejaIds);
}
