package com.example.base_api.usuario.repository;

import com.example.base_api.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPessoaId(UUID pessoaId);

    @Query("""
            SELECT DISTINCT u FROM Usuario u
            JOIN u.papeis p
            WHERE UPPER(p.nome) = UPPER(:papel)
              AND u.ativo = true
            ORDER BY u.username
            """)
    List<Usuario> findAtivosByPapel(@Param("papel") String papel);
}
