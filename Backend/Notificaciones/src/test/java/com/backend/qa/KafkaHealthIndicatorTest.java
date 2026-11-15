package com.backend.qa;

import com.backend.monitoring.KafkaHealthIndicator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Monitoreo: el health check de Kafka refleja si el broker responde, con timeout corto. */
class KafkaHealthIndicatorTest {

    private static KafkaHealthIndicator indicador(String bootstrap, SimpleMeterRegistry registry) {
        return new KafkaHealthIndicator(
                new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap)), registry);
    }

    @Test
    void sinBroker_DOWN_rapido_yGaugeEnCero() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        KafkaHealthIndicator h = indicador("localhost:1", registry);
        try {
            long inicio = System.currentTimeMillis();
            Health health = h.health();
            long ms = System.currentTimeMillis() - inicio;
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsKey("error");
            assertThat(ms).as("timeout corto").isLessThan(8000);
            assertThat(registry.get("nexus.kafka.broker.disponible").gauge().value()).isZero();
        } finally {
            h.destroy();
        }
    }

    @Test
    void conBroker_UP_conClusterId_yGaugeEnUno() throws Exception {
        EmbeddedKafkaBroker broker = new EmbeddedKafkaKraftBroker(1, 1);
        broker.afterPropertiesSet();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        KafkaHealthIndicator h = indicador(broker.getBrokersAsString(), registry);
        try {
            Health health = h.health();
            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsKeys("clusterId", "brokers");
            assertThat(health.getDetails().get("brokers")).isEqualTo(1);
            assertThat(registry.get("nexus.kafka.broker.disponible").gauge().value()).isEqualTo(1.0);
        } finally {
            h.destroy();
            broker.destroy();
        }
    }
}