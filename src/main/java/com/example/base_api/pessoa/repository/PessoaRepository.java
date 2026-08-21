package com.example.base_api.pessoa.repository;

import com.example.base_api.pessoa.entity.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PessoaRepository extends JpaRepository<Pessoa, UUID> {

    Optional<Pessoa> findByEmailIgnoreCase(String email);

    List<Pessoa> findByAtivoTrueOrderByNomeAsc();

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
}
