   package com.backend.config;

   import org.springframework.boot.context.properties.ConfigurationProperties;

   @ConfigurationProperties(prefix = "app.messaging")
   public record MensajeriaProperties(
           Exchanges exchanges,
           Queues queues,
           Dlq dlq,
           RoutingKeys routingKeys,
           long ttlMs,
           long dlqTtlMs,
           int maxReintentos) {

       public record Exchanges(String direct, String topic, String dlx) {}

       public record Queues(String notificaciones, String tickets, String inventario) {}

       public record Dlq(String notificaciones, String tickets, String inventario) {}

       public record RoutingKeys(
               String usuarioRegistrado,
               String ordenCreada,
               String productoPatron,
               String productoCreado,
               String productoActualizado,
               String productoEliminado,
               String productoStockBajo) {}
   }