package com.backend.qa;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Monitoreo: cada mensaje enviado al DLT suma en nexus_kafka_dlt_enviados_total{topic,excepcion}. */
@SpringBootTest(properties = {"spring.kafka.listener.auto-startup=true", "management.health.kafka.enabled=false"})
@EmbeddedKafka(partitions = 3, bootstrapServersProperty = "spring.kafka.bootstrap-servers",
        topics = {"nexus.usuario.registrado", "nexus.orden.creada", "nexus.producto.eventos",
                "nexus.usuario.registrado.DLT", "nexus.orden.creada.DLT", "nexus.producto.eventos.DLT"})
@DirtiesContext
class MetricasDltEmbeddedTest {

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void mensajeInvalido_incrementaContadorDlt_yGaugesRegistrados() throws Exception {
        for (MessageListenerContainer c : registry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(c, 3);
        }
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>("nexus.usuario.registrado", "sin-email",
                    "{\"azureOid\":\"x\",\"nombre\":\"Ana\",\"email\":\"\",\"fecha\":\"2026-10-17T20:15:00Z\"}")).get();
        }
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Counter c = meterRegistry.find("nexus.kafka.dlt.enviados")
                    .tag("topic", "nexus.usuario.registrado.DLT")
                    .tag("excepcion", "MensajeInvalidoException")
                    .counter();
            assertThat(c).isNotNull();
            assertThat(c.count()).isEqualTo(1.0);
        });
        assertThat(meterRegistry.find("nexus.kafka.dlt.mensajes").gauges()).hasSize(3);
        assertThat(meterRegistry.find("nexus.servicio.salud").gauge()).isNotNull();
    }
}