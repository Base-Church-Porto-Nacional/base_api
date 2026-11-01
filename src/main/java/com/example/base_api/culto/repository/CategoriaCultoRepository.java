package com.example.base_api.culto.repository;

import com.example.base_api.culto.entity.CategoriaCulto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaCultoRepository extends JpaRepository<CategoriaCulto, UUID> {

    List<CategoriaCulto> findByIgrejaIdInOrderByTituloAsc(List<UUID> igrejaIds);

    List<CategoriaCulto> findByIgrejaIdInAndAtivoOrderByTituloAsc(List<UUID> igrejaIds, boolean ativo);

    Optional<CategoriaCulto> findByIgrejaIdAndTituloIgnoreCase(UUID igrejaId, String titulo);

    boolean existsByIgrejaIdAndTituloIgnoreCase(UUID igrejaId, String titulo);

    boolean existsByIgrejaIdAndTituloIgnoreCaseAndIdNot(UUID igrejaId, String titulo, UUID id);
}