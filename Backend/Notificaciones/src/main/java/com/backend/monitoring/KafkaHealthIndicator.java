package com.backend.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Health check de Kafka: aparece como "kafka" en /actuator/health.
 * Consulta el cluster (describeCluster) con un timeout corto; si el broker no responde, el servicio queda DOWN (503).
 * Tambien publica el gauge nexus_kafka_broker_disponible (1 = conectado, 0 = sin conexion).
 * Se desactiva con management.health.kafka.enabled=false.
 */
@Component("kafkaHealthIndicator")
@ConditionalOnEnabledHealthIndicator("kafka")
public class KafkaHealthIndicator implements HealthIndicator, DisposableBean {

    static final int TIMEOUT_MS = 3000;

    private final AdminClient adminClient;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin, MeterRegistry registry) {
        Map<String, Object> config = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, TIMEOUT_MS);
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, TIMEOUT_MS);
        config.put(AdminClientConfig.CLIENT_ID_CONFIG, "health-check");
        this.adminClient = AdminClient.create(config);

        Gauge.builder("nexus.kafka.broker.disponible", () -> Status.UP.equals(health().getStatus()) ? 1 : 0)
                .description("1 si el servicio logra conectarse al broker Kafka, 0 si no")
                .register(registry);
    }

    @Override
    public Health health() {
        try {
            DescribeClusterResult cluster = adminClient.describeCluster(new DescribeClusterOptions().timeoutMs(TIMEOUT_MS));
            String clusterId = cluster.clusterId().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            int brokers = cluster.nodes().get(TIMEOUT_MS, TimeUnit.MILLISECONDS).size();
            Health.Builder builder = brokers > 0 ? Health.up() : Health.down();
            return builder.withDetail("clusterId", clusterId).withDetail("brokers", brokers).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Health.down().withDetail("error", "interrumpido").build();
        } catch (Exception e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            return Health.down().withDetail("error", causa.getClass().getSimpleName() + ": " + causa.getMessage()).build();
        }
    }

    @Override
    public void destroy() {
        adminClient.close(Duration.ofSeconds(2));
    }
}