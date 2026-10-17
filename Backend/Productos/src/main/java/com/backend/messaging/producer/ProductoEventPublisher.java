   package com.backend.messaging.producer;

   /** Publica los eventos de inventario. ProductoService solo conoce esta interfaz. */
   public interface ProductoEventPublisher {

       void productoCreado(Long productoId, String nombre, Integer stock);

       void productoActualizado(Long productoId, String nombre, Integer stock);

       void productoEliminado(Long productoId, String nombre);

       void stockBajo(Long productoId, String nombre, Integer stock);
   }