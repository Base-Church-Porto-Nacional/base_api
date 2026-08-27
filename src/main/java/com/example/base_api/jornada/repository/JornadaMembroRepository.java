package com.example.base_api.jornada.repository;

import com.example.base_api.jornada.entity.JornadaMembro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JornadaMembroRepository extends JpaRepository<JornadaMembro, UUID> {

    Optional<JornadaMembro> findByPessoaId(UUID pessoaId);

    List<JornadaMembro> findByPessoaIdIn(List<UUID> pessoaIds);
}
