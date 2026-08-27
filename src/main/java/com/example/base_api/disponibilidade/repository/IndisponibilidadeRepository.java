package com.example.base_api.disponibilidade.repository;

import com.example.base_api.disponibilidade.entity.Indisponibilidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IndisponibilidadeRepository extends JpaRepository<Indisponibilidade, UUID> {

    List<Indisponibilidade> findByPessoaId(UUID pessoaId);

    List<Indisponibilidade> findByPessoaIdIn(List<UUID> pessoaIds);
}
