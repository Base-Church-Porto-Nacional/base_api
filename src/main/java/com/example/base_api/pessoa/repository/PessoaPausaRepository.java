package com.example.base_api.pessoa.repository;

import com.example.base_api.pessoa.entity.PessoaPausa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PessoaPausaRepository extends JpaRepository<PessoaPausa, UUID> {

    List<PessoaPausa> findByPessoaIdOrderByDataInicioDesc(UUID pessoaId);

    List<PessoaPausa> findByPessoaIdInAndAtivaTrue(List<UUID> pessoaIds);

    long countByPessoaIdInAndAtivaTrue(List<UUID> pessoaIds);
}
