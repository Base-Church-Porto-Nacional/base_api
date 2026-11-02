package com.example.base_api.usuario.repository;

import com.example.base_api.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPessoaId(UUID pessoaId);

    Optional<Usuario> findByPessoaId(UUID pessoaId);

    List<Usuario> findByPessoaIdIn(Collection<UUID> pessoaIds);

    List<Usuario> findByIgrejaIdIn(List<UUID> igrejaIds);

    long countByIgrejaIdIn(List<UUID> igrejaIds);

    @Modifying
    @Query("delete from Usuario u where u.id = :id")
    int excluirDireto(@Param("id") UUID id);

    /** Usuários da igreja que possuem o papel de SISTEMA indicado (ex.: PASTOR). */
    @Query("""
            SELECT COUNT(DISTINCT u) FROM Usuario u
            JOIN u.papeisIgreja up
            JOIN up.papel p
            WHERE p.nome = :nome
              AND p.igrejaId IS NULL
              AND up.igrejaId = :igrejaId
            """)
    long countByPapelAndIgrejaId(@Param("nome") String nome, @Param("igrejaId") UUID igrejaId);
}
