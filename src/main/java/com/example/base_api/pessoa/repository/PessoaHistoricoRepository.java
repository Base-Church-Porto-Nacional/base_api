package com.example.base_api.pessoa.repository;

import com.example.base_api.pessoa.entity.PessoaHistorico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PessoaHistoricoRepository extends JpaRepository<PessoaHistorico, UUID> {

    List<PessoaHistorico> findByPessoaIdOrderByDataRegistroDescCreatedAtDesc(UUID pessoaId);
}
