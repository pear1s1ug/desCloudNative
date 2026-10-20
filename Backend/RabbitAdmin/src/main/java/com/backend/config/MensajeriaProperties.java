package com.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchanges, colas, DLQ y routing keys de la mensajería.
 * Se leen desde el bloque app.messaging de application.yml: en el código
 * no se escriben nombres a mano.
 */
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