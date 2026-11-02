package com.example.base_api.smallgroup.repository;

import com.example.base_api.smallgroup.entity.SmallGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SmallGroupRepository extends JpaRepository<SmallGroup, UUID> {

    Optional<SmallGroup> findByIgrejaIdAndNomeIgnoreCase(UUID igrejaId, String nome);

    boolean existsByIgrejaIdAndNomeIgnoreCaseAndIdNot(UUID igrejaId, String nome, UUID id);

    List<SmallGroup> findByIgrejaIdInOrderByNomeAsc(List<UUID> igrejaIds);

    List<SmallGroup> findByIgrejaIdAndLiderPessoaId(UUID igrejaId, UUID liderPessoaId);

    @org.springframework.data.jpa.repository.Query("select distinct g.liderPessoaId from SmallGroup g where g.igrejaId in :igrejaIds and g.liderPessoaId is not null")
    List<UUID> idsLideres(@org.springframework.data.repository.query.Param("igrejaIds") java.util.Collection<UUID> igrejaIds);
}
