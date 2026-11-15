package com.backend.consumer.soporte;

import com.backend.config.MensajeriaProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Alerta de mensajes fallidos: cada 30 s revisa los topics DLT y registra un WARN si tienen mensajes.
 * Los DLT no se consumen automaticamente, para que los mensajes queden disponibles para analisis.
 * Expone el gauge nexus_kafka_dlt_mensajes{topic="..."} (mensajes guardados en cada DLT; -1 = no se pudo revisar).
 */
@Component
public class DlqMonitor {

    private static final Logger log = LoggerFactory.getLogger(DlqMonitor.class);

    private final KafkaAdmin kafkaAdmin;
    private final List<String> dlts;
    private final Map<String, AtomicLong> mensajesPorDlt = new LinkedHashMap<>();

    public DlqMonitor(KafkaAdmin kafkaAdmin, MensajeriaProperties props, MeterRegistry registry) {
        this.kafkaAdmin = kafkaAdmin;
        this.dlts = List.of(
                props.dlt().usuarioRegistrado(),
                props.dlt().ordenCreada(),
                props.dlt().productoEventos());
        for (String dlt : dlts) {
            AtomicLong valor = new AtomicLong(0);
            mensajesPorDlt.put(dlt, valor);
            Gauge.builder("nexus.kafka.dlt.mensajes", valor, AtomicLong::get)
                    .description("Mensajes guardados en el topic DLT (revisado cada 30 s por DlqMonitor)")
                    .tag("topic", dlt)
                    .strongReference(true)
                    .register(registry);
        }
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void revisarDlts() {
        try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            for (String dlt : dlts) {
                long mensajes = contarMensajes(admin, dlt);
                mensajesPorDlt.get(dlt).set(mensajes);
                if (mensajes > 0) {
                    log.warn("[ALERTA DLT] {} tiene {} mensaje(s) sin procesar", dlt, mensajes);
                }
            }
        } catch (Exception e) {
            mensajesPorDlt.values().forEach(v -> v.set(-1));
            log.warn("No se pudieron revisar los topics DLT: {}", e.getMessage());
        }
    }

    private long contarMensajes(AdminClient admin, String topic) throws Exception {
        Map<String, TopicDescription> descriptions = admin.describeTopics(List.of(topic))
                .allTopicNames()
                .get(10, TimeUnit.SECONDS);
        TopicDescription description = descriptions.get(topic);
        if (description == null) {
            return 0;
        }

        Map<TopicPartition, OffsetSpec> earliest = new HashMap<>();
        Map<TopicPartition, OffsetSpec> latest = new HashMap<>();
        description.partitions().forEach(p -> {
            TopicPartition tp = new TopicPartition(topic, p.partition());
            earliest.put(tp, OffsetSpec.earliest());
            latest.put(tp, OffsetSpec.latest());
        });

        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> start =
                admin.listOffsets(earliest).all().get(10, TimeUnit.SECONDS);
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> end =
                admin.listOffsets(latest).all().get(10, TimeUnit.SECONDS);

        long total = 0;
        for (TopicPartition tp : earliest.keySet()) {
            total += end.get(tp).offset() - start.get(tp).offset();
        }
        return total;
    }
}