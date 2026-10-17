   package com.backend.model;

   import jakarta.persistence.*;
   import lombok.*;

   @Entity
   @Table(name = "detalle_ordenes")
   @Getter
   @Setter
   @NoArgsConstructor
   @AllArgsConstructor
   @Builder
   public class DetalleOrdenModel {

       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;

       @Column(name = "producto_id", nullable = false)
       private Long productoId;

       @Column(nullable = false)
       private Integer cantidad;

       @Column(name = "precio_unitario", nullable = false)
       private Double precioUnitario;
   }