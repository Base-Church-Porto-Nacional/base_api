package com.example.base_api.usuario.repository;

import com.example.base_api.security.Identificador;
import com.example.base_api.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findByTelefone(String telefone);

    boolean existsByTelefone(String telefone);

    /** Login: e-mail (contém "@") ou telefone em qualquer formato aceito pelo normalizador. */
    default Optional<Usuario> findByIdentificador(String bruto) {
        Optional<String> email = Identificador.email(bruto);
        if (email.isPresent()) {
            return findByEmailIgnoreCase(email.get());
        }
        return Identificador.telefone(bruto).flatMap(this::findByTelefone);
    }

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPessoaId(UUID pessoaId);

    Optional<Usuario> findByPessoaId(UUID pessoaId);

    List<Usuario> findByPessoaIdIn(Collection<UUID> pessoaIds);

    List<Usuario> findByIgrejaIdIn(List<UUID> igrejaIds);

    long countByIgrejaIdIn(List<UUID> igrejaIds);

    /** Usuários ATIVOS da igreja que possuem o papel de SISTEMA indicado (ex.: PASTOR). */
    @Query("""
            SELECT COUNT(DISTINCT u) FROM Usuario u
            JOIN u.papeisIgreja up
            JOIN up.papel p
            WHERE p.nome = :nome
              AND p.igrejaId IS NULL
              AND up.igrejaId = :igrejaId
              AND u.status = com.example.base_api.usuario.entity.StatusUsuario.ATIVO
            """)
    long countByPapelAndIgrejaId(@Param("nome") String nome, @Param("igrejaId") UUID igrejaId);
}
