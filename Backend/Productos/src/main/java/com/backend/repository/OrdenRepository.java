   package com.backend.repository;

   import com.backend.model.OrdenModel;
   import org.springframework.data.jpa.repository.JpaRepository;
   import org.springframework.data.jpa.repository.Query;
   import org.springframework.data.repository.query.Param;

   import java.util.Optional;

   public interface OrdenRepository extends JpaRepository<OrdenModel, Long> {

       /** Id interno del usuario a partir de su Object ID de Azure (claim "oid"). */
       @Query(value = "SELECT CAST(id AS BIGINT) FROM usuarios WHERE azure_oid = :oid", nativeQuery = true)
       Optional<Long> buscarUsuarioIdPorOid(@Param("oid") String oid);
   }