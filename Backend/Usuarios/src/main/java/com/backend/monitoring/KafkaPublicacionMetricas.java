package com.backend.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Cuenta los eventos publicados en Kafka por topic y resultado:
 * nexus_kafka_eventos_publicados_total{topic="...", resultado="ok|error"}.
 * Tambien registra en el log los envios que fallan (antes se perdian sin aviso).
 */
@Component
public class KafkaPublicacionMetricas {

    private static final Logger log = LoggerFactory.getLogger(KafkaPublicacionMetricas.class);

    private final MeterRegistry registry;

    public KafkaPublicacionMetricas(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Deja los contadores en 0 desde el arranque para que Grafana muestre la serie aunque no haya envios. */
    public void registrarTopic(String topic) {
        contador(topic, "ok");
        contador(topic, "error");
    }

    public void monitorear(CompletableFuture<? extends SendResult<?, ?>> envio, String topic, String key) {
        if (envio == null) {
            return; // KafkaTemplate simulado en tests unitarios
        }
        envio.whenComplete((resultado, error) -> {
            if (error == null) {
                contador(topic, "ok").increment();
                if (resultado != null && resultado.getRecordMetadata() != null) {
                    log.debug("Kafka confirmo {} key {} en particion {} offset {}", topic, key,
                            resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset());
                }
            } else {
                contador(topic, "error").increment();
                log.error("No se pudo publicar en Kafka: topic {} key {}: {}", topic, key, error.getMessage());
            }
        });
    }

    public void error(String topic) {
        contador(topic, "error").increment();
    }

    private Counter contador(String topic, String resultado) {
        return Counter.builder("nexus.kafka.eventos.publicados")
                .description("Eventos publicados en Kafka por topic y resultado")
                .tag("topic", topic)
                .tag("resultado", resultado)
                .register(registry);
    }
}