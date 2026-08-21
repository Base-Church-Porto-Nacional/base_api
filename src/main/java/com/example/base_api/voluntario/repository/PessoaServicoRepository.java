package com.example.base_api.voluntario.repository;

import com.example.base_api.voluntario.entity.PessoaServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PessoaServicoRepository extends JpaRepository<PessoaServico, UUID> {

    List<PessoaServico> findByPessoaId(UUID pessoaId);

    List<PessoaServico> findByServicoIdAndAtivoTrue(UUID servicoId);

    Optional<PessoaServico> findByPessoaIdAndServicoId(UUID pessoaId, UUID servicoId);

    boolean existsByPessoaIdAndServicoId(UUID pessoaId, UUID servicoId);
}
