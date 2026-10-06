package com.example.base_api.aniversario.repository;

import com.example.base_api.aniversario.entity.MensagemAniversario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MensagemAniversarioRepository extends JpaRepository<MensagemAniversario, UUID> {

    Optional<MensagemAniversario> findByIgrejaId(UUID igrejaId);
}
