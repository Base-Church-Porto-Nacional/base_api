package com.example.base_api.jornada.repository;

import com.example.base_api.jornada.entity.PessoaJornadaEtapa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PessoaJornadaEtapaRepository extends JpaRepository<PessoaJornadaEtapa, UUID> {

    List<PessoaJornadaEtapa> findByPessoaId(UUID pessoaId);

    Optional<PessoaJornadaEtapa> findByPessoaIdAndEtapaId(UUID pessoaId, UUID etapaId);
}
