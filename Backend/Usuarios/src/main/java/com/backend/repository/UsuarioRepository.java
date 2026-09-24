package com.backend.repository;

import com.backend.model.UsuarioModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ==========================================
 * CAPA DE REPOSITORIO (ACCESO A DATOS - MODEL)
 * ==========================================
 */
@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioModel, Long> {

    Optional<UsuarioModel> findByAzureOid(String azureOid);
}