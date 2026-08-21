package com.example.base_api.disponibilidade.repository;

import com.example.base_api.disponibilidade.entity.DisponibilidadeSemanal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisponibilidadeSemanalRepository extends JpaRepository<DisponibilidadeSemanal, UUID> {

    List<DisponibilidadeSemanal> findByPessoaId(UUID pessoaId);
}
