package com.example.base_api.pessoa.repository;

import com.example.base_api.pessoa.entity.PessoaPausa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PessoaPausaRepository extends JpaRepository<PessoaPausa, UUID> {

    List<PessoaPausa> findByPessoaIdOrderByDataInicioDesc(UUID pessoaId);

    List<PessoaPausa> findByPessoaIdInAndAtivaTrue(List<UUID> pessoaIds);

    @Query("""
            select count(pp) from PessoaPausa pp, Pessoa p
            where p.id = pp.pessoaId and p.igrejaId in :igrejaIds and pp.ativa = true
            """)
    long contarAtivasPorIgrejas(@Param("igrejaIds") Collection<UUID> igrejaIds);
}
