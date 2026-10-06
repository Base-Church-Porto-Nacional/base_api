package com.example.base_api.aniversario.repository;

import com.example.base_api.aniversario.entity.AniversarioEnvio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AniversarioEnvioRepository extends JpaRepository<AniversarioEnvio, UUID> {

    boolean existsByPessoaIdAndDataAniversario(UUID pessoaId, LocalDate dataAniversario);

    List<AniversarioEnvio> findByIgrejaIdInAndDataAniversarioBetweenOrderByDataProcessamentoDesc(
            List<UUID> igrejaIds, LocalDate inicio, LocalDate fim
    );
}