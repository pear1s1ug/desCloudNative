   package com.backend.messaging.event;

   import java.time.Instant;

   public record ProductoEvent(
           Long productoId,
           String nombre,
           Integer stock,
           String accion,   // CREADO | ACTUALIZADO | ELIMINADO | STOCK_BAJO
           Instant fecha) {}