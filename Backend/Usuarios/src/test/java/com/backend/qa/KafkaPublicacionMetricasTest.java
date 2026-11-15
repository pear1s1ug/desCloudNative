package com.backend.qa;

import com.backend.monitoring.KafkaPublicacionMetricas;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/** Monitoreo: contador nexus_kafka_eventos_publicados_total por topic y resultado. */
class KafkaPublicacionMetricasTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final KafkaPublicacionMetricas metricas = new KafkaPublicacionMetricas(registry);

    private double valor(String topic, String resultado) {
        return registry.get("nexus.kafka.eventos.publicados").tag("topic", topic).tag("resultado", resultado).counter().count();
    }

    @Test
    void registrarTopic_dejaContadoresEnCero() {
        metricas.registrarTopic("t1");
        assertThat(valor("t1", "ok")).isZero();
        assertThat(valor("t1", "error")).isZero();
    }

    @Test
    void envioConfirmado_sumaOk() {
        CompletableFuture<SendResult<String, String>> f = new CompletableFuture<>();
        metricas.monitorear(f, "t1", "k");
        f.complete(new SendResult<>(null, null));
        assertThat(valor("t1", "ok")).isEqualTo(1);
    }

    @Test
    void envioFallido_sumaError() {
        CompletableFuture<SendResult<String, String>> f = new CompletableFuture<>();
        metricas.monitorear(f, "t1", "k");
        f.completeExceptionally(new RuntimeException("broker caido"));
        assertThat(valor("t1", "error")).isEqualTo(1);
    }

    @Test
    void futureNulo_noFalla() {
        metricas.monitorear(null, "t1", "k");
        metricas.error("t1");
        assertThat(valor("t1", "error")).isEqualTo(1);
    }
}