package com.example.base_api.usuario.repository;

import com.example.base_api.usuario.entity.UsuarioPapel;
import com.example.base_api.usuario.entity.UsuarioPapelId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UsuarioPapelRepository extends JpaRepository<UsuarioPapel, UsuarioPapelId> {

    List<UsuarioPapel> findByUsuarioId(UUID usuarioId);

    @Modifying
    @Query("DELETE FROM UsuarioPapel up WHERE up.usuarioId = :usuarioId AND up.igrejaId = :igrejaId")
    void deleteByUsuarioIdAndIgrejaId(@Param("usuarioId") UUID usuarioId, @Param("igrejaId") UUID igrejaId);
}
