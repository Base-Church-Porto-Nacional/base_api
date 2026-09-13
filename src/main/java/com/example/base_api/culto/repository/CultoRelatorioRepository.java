package com.example.base_api.culto.repository;

import com.example.base_api.culto.entity.CultoRelatorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CultoRelatorioRepository extends JpaRepository<CultoRelatorio, UUID> {

    Optional<CultoRelatorio> findByCultoId(UUID cultoId);

    boolean existsByCultoId(UUID cultoId);

    List<CultoRelatorio> findByCultoIdIn(Collection<UUID> cultoIds);
}