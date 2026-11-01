package com.example.base_api.pessoa.repository;

import com.example.base_api.pessoa.entity.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public interface PessoaRepository extends JpaRepository<Pessoa, UUID> {

    /** Nomes das pessoas por id, numa única consulta. Ids nulos são ignorados. */
    default Map<UUID, String> nomesPorId(Collection<UUID> ids) {
        List<UUID> validos = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (validos.isEmpty()) {
            return Map.of();
        }
        return findAllById(validos).stream().collect(Collectors.toMap(Pessoa::getId, Pessoa::getNome));
    }

    /**
     * Aniversariantes de um dia (considera apenas dia e mês da data de nascimento).
     * Pessoas sem data de nascimento são ignoradas pelo próprio filtro.
     */
    @Query("""
            select p from Pessoa p
            where p.ativo = true
              and p.dataNascimento is not null
              and extract(day from p.dataNascimento) = :dia
              and extract(month from p.dataNascimento) = :mes
            order by p.nome asc
            """)
    List<Pessoa> findAniversariantesDoDia(@Param("dia") int dia, @Param("mes") int mes);

    Optional<Pessoa> findByEmailIgnoreCase(String email);

    List<Pessoa> findByIgrejaIdInOrderByNomeAsc(List<UUID> igrejaIds);

    long countByIgrejaIdIn(List<UUID> igrejaIds);

    long countByIgrejaIdInAndAtivoTrue(List<UUID> igrejaIds);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
}
