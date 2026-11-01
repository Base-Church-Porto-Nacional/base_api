package com.example.base_api.smallgroup.repository;

import com.example.base_api.smallgroup.entity.SmallGroupEncontro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SmallGroupEncontroRepository extends JpaRepository<SmallGroupEncontro, UUID> {

    List<SmallGroupEncontro> findBySmallGroupIdInOrderByDataAsc(List<UUID> smallGroupIds);

    Optional<SmallGroupEncontro> findFirstBySmallGroupIdAndData(UUID smallGroupId, LocalDate data);

}