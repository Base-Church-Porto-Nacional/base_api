package com.example.base_api.servico.repository;

import com.example.base_api.servico.entity.Servico;
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

public interface ServicoRepository extends JpaRepository<Servico, UUID> {

    /** Nomes dos serviços por id, numa única consulta. Ids nulos são ignorados. */
    default Map<UUID, String> nomesPorId(Collection<UUID> ids) {
        List<UUID> validos = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (validos.isEmpty()) {
            return Map.of();
        }
        return findAllById(validos).stream().collect(Collectors.toMap(Servico::getId, Servico::getNome));
    }

    List<Servico> findByMinisterioIdOrderByNomeAsc(UUID ministerioId);

    void deleteByMinisterioId(UUID ministerioId);

    List<Servico> findByMinisterioIdIn(List<UUID> ministerioIds);

    List<Servico> findByMinisterioIdInOrderByNomeAsc(Collection<UUID> ministerioIds);

    List<Servico> findByMinisterioIdInAndAtivoTrueOrderByNomeAsc(List<UUID> ministerioIds);

    Optional<Servico> findByMinisterioIdAndNomeIgnoreCase(UUID ministerioId, String nome);

    boolean existsByMinisterioIdAndNomeIgnoreCaseAndIdNot(UUID ministerioId, String nome, UUID id);

    @Query("""
            select count(s) from Servico s, Ministerio m
            where m.id = s.ministerioId and m.igrejaId in :igrejaIds
            """)
    long contarPorIgrejas(@Param("igrejaIds") Collection<UUID> igrejaIds);
}
