package com.example.base_api.igreja.repository;

import com.example.base_api.igreja.entity.Igreja;
import com.example.base_api.igreja.entity.IgrejaTipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IgrejaRepository extends JpaRepository<Igreja, UUID> {

    Optional<Igreja> findFirstByTipo(IgrejaTipo tipo);

    List<Igreja> findByIgrejaMatrizIdOrderByNomeAsc(UUID igrejaMatrizId);

    List<Igreja> findByAtivoTrueOrderByNomeAsc();
}
