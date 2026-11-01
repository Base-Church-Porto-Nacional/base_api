package com.example.base_api.smallgroup.repository;

import com.example.base_api.smallgroup.entity.SmallGroupParticipante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SmallGroupParticipanteRepository extends JpaRepository<SmallGroupParticipante, UUID> {

    List<SmallGroupParticipante> findByEncontroIdInOrderByPessoaNomeAsc(List<UUID> encontroIds);

    List<SmallGroupParticipante> findByEncontroIdOrderByPessoaNomeAsc(UUID encontroId);

    void deleteByEncontroId(UUID encontroId);
}