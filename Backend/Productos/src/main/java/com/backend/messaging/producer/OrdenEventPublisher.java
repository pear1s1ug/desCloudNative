   package com.backend.messaging.producer;

   import com.backend.messaging.event.OrdenCreadaEvent;

   public interface OrdenEventPublisher {

       void ordenCreada(OrdenCreadaEvent evento);
   }