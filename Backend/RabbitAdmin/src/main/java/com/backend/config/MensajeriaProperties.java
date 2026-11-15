package com.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
public record MensajeriaProperties(
        Topics topics,
        Dlt dlt,
        ConsumerGroups consumerGroups,
        int partitions,
        short replicationFactor,
        int maxReintentos) {

    public record Topics(String usuarioRegistrado, String ordenCreada, String productoEventos) {}

    public record Dlt(String usuarioRegistrado, String ordenCreada, String productoEventos) {}

    public record ConsumerGroups(String notificaciones, String tickets, String inventario) {}
}
