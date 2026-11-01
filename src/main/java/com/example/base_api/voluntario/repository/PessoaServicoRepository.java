package com.example.base_api.voluntario.repository;

import com.example.base_api.voluntario.entity.PessoaServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PessoaServicoRepository extends JpaRepository<PessoaServico, UUID> {

    List<PessoaServico> findByPessoaId(UUID pessoaId);

    List<PessoaServico> findByPessoaIdIn(List<UUID> pessoaIds);

    List<PessoaServico> findByServicoIdAndAtivoTrue(UUID servicoId);

    List<PessoaServico> findByServicoIdInAndAtivoTrue(List<UUID> servicoIds);

    Optional<PessoaServico> findByPessoaIdAndServicoId(UUID pessoaId, UUID servicoId);

    boolean existsByPessoaIdAndServicoId(UUID pessoaId, UUID servicoId);

    void deleteByServicoIdIn(List<UUID> servicoIds);

    @Query("""
            select count(ps) from PessoaServico ps, Pessoa p
            where p.id = ps.pessoaId and p.igrejaId in :igrejaIds and ps.ativo = true
            """)
    long contarAtivosPorIgrejas(@Param("igrejaIds") Collection<UUID> igrejaIds);
}
