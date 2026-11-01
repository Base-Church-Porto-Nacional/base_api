package com.example.base_api.igreja.repository;

import com.example.base_api.igreja.entity.Igreja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IgrejaRepository extends JpaRepository<Igreja, UUID> {

    List<Igreja> findByIgrejaMatrizIdOrderByNomeAsc(UUID igrejaMatrizId);

    List<Igreja> findByAtivoTrueOrderByNomeAsc();
}
