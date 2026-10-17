   package com.backend.model;

   import jakarta.persistence.*;
   import lombok.*;

   import java.time.LocalDateTime;
   import java.util.ArrayList;
   import java.util.List;

   @Entity
   @Table(name = "ordenes")
   @Getter
   @Setter
   @NoArgsConstructor
   @AllArgsConstructor
   @Builder
   public class OrdenModel {

       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;

       /** FK a usuarios.id (el usuario se busca por su azure_oid). */
       @Column(name = "usuario_id", nullable = false)
       private Long usuarioId;

       @Column(nullable = false)
       private LocalDateTime fecha;

       @Column(nullable = false)
       private Double total;

       /** Enum de PostgreSQL (ordenes_estado): se deja el valor por defecto de la BD, 'PENDIENTE'. */
       @Column(insertable = false, updatable = false)
       private String estado;

       @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
       @JoinColumn(name = "orden_id", nullable = false)
       @Builder.Default
       private List<DetalleOrdenModel> detalles = new ArrayList<>();
   }