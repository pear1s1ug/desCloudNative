package com.backend.qa;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * QA Kafka: flujo real de los 3 consumidores contra un broker embebido (no usa el broker de Docker).
 * Verifica deserializacion JSON, procesamiento, reintentos (FixedBackOff 1s x 3) y envio a DLT.
 */
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=true")
@EmbeddedKafka(partitions = 3,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers",
        topics = {"nexus.usuario.registrado", "nexus.orden.creada", "nexus.producto.eventos",
                "nexus.usuario.registrado.DLT", "nexus.orden.creada.DLT", "nexus.producto.eventos.DLT"})
@DirtiesContext
class KafkaConsumidoresEmbeddedTest {

    static final Contador CONTADOR = new Contador();

    /** Cuenta cada entrega al listener (incluye reintentos) y los procesamientos exitosos, por key. */
    static class Contador implements RecordInterceptor<Object, Object> {
        final Map<String, AtomicInteger> intentos = new ConcurrentHashMap<>();
        final Set<String> exitos = ConcurrentHashMap.newKeySet();

        @Override
        public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record,
                                                        Consumer<Object, Object> consumer) {
            intentos.computeIfAbsent(String.valueOf(record.key()), k -> new AtomicInteger()).incrementAndGet();
            return record;
        }

        @Override
        public void success(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
            exitos.add(String.valueOf(record.key()));
        }

        int intentos(String key) {
            AtomicInteger n = intentos.get(key);
            return n == null ? 0 : n.get();
        }
    }

    @TestConfiguration
    static class QaConfig {
        @Bean
        RecordInterceptor<Object, Object> qaRecordInterceptor() {
            return CONTADOR;
        }
    }

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @BeforeEach
    void esperarAsignacion() {
        assertThat(registry.getListenerContainers()).hasSize(3);
        for (MessageListenerContainer c : registry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(c, 3);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static String key(String prefijo) {
        return prefijo + "-" + UUID.randomUUID();
    }

    private void enviar(String topic, String key, String json) throws Exception {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(topic, key, json)).get();
        }
    }

    private ConsumerRecord<String, String> buscarEnDlt(String dlt, String key, Duration timeout) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "qa-dlt-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(dlt));
            long fin = System.currentTimeMillis() + timeout.toMillis();
            while (System.currentTimeMillis() < fin) {
                for (ConsumerRecord<String, String> r : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(r.key())) {
                        return r;
                    }
                }
            }
        }
        return null;
    }

    private static String header(ConsumerRecord<String, String> r, String nombre) {
        Header h = r.headers().lastHeader(nombre);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }

    private void esperarExito(String key) {
        await().atMost(Duration.ofSeconds(30)).until(() -> CONTADOR.exitos.contains(key));
    }

    // ---------------------------------------------------------------- casos felices

    @Test
    void usuarioRegistrado_valido_seProcesaUnaVez() throws Exception {
        String k = key("usr-ok");
        enviar("nexus.usuario.registrado", k,
                "{\"azureOid\":\"" + k + "\",\"nombre\":\"Ana\",\"email\":\"ana@test.cl\",\"fecha\":\"2026-10-17T20:15:00Z\"}");
        esperarExito(k);
        assertThat(CONTADOR.intentos(k)).isEqualTo(1);
    }

    @Test
    void ordenCreada_valida_generaTicket() throws Exception {
        String k = key("ord-ok");
        enviar("nexus.orden.creada", k,
                "{\"ordenId\":15,\"usuarioOid\":\"oid\",\"items\":[{\"productoId\":1,\"nombre\":\"Teclado\",\"cantidad\":1,"
                        + "\"precioUnitario\":89990.0}],\"total\":89990.0,\"fecha\":\"2026-10-17T20:15:00Z\"}");
        esperarExito(k);
        assertThat(CONTADOR.intentos(k)).isEqualTo(1);
    }

    @Test
    void productoEvento_stockBajo_seProcesa() throws Exception {
        String k = key("prd-ok");
        enviar("nexus.producto.eventos", k,
                "{\"productoId\":7,\"nombre\":\"Mouse\",\"stock\":2,\"accion\":\"STOCK_BAJO\",\"fecha\":\"2026-10-17T20:15:00Z\"}");
        esperarExito(k);
    }

    @Test
    void compatibilidad_camposExtra_y_fechaNumerica_seAceptan() throws Exception {
        String k1 = key("usr-extra");
        enviar("nexus.usuario.registrado", k1,
                "{\"azureOid\":\"a\",\"nombre\":\"Ana\",\"email\":\"a@test.cl\",\"fecha\":\"2026-10-17T20:15:00Z\",\"nuevo\":1}");
        // formato que hoy emiten Productos/Usuarios (epoch con decimales, ver DEF-02)
        String k2 = key("usr-epoch");
        enviar("nexus.usuario.registrado", k2,
                "{\"azureOid\":\"b\",\"nombre\":\"Ana\",\"email\":\"b@test.cl\",\"fecha\":1794277248.458217200}");
        esperarExito(k1);
        esperarExito(k2);
    }

    // ---------------------------------------------------------------- errores y DLT

    @Test
    void usuarioSinEmail_vaDirectoADlt_sinReintentos() throws Exception {
        String k = key("usr-inv");
        enviar("nexus.usuario.registrado", k,
                "{\"azureOid\":\"x\",\"nombre\":\"Ana\",\"email\":\"\",\"fecha\":\"2026-10-17T20:15:00Z\"}");
        ConsumerRecord<String, String> dlt = buscarEnDlt("nexus.usuario.registrado.DLT", k, Duration.ofSeconds(30));
        assertThat(dlt).as("mensaje en DLT").isNotNull();
        assertThat(header(dlt, "kafka_dlt-exception-cause-fqcn")).endsWith("MensajeInvalidoException");
        assertThat(header(dlt, "kafka_dlt-original-topic")).isEqualTo("nexus.usuario.registrado");
        assertThat(header(dlt, "kafka_dlt-original-consumer-group")).isEqualTo("nexus.notificaciones");
        assertThat(CONTADOR.intentos(k)).isEqualTo(1);
        assertThat(CONTADOR.exitos).doesNotContain(k);
    }

    @Test
    void ordenConFallaTransitoria_seReintenta3Veces_yLuegoDlt() throws Exception {
        String k = key("ord-falla");
        enviar("nexus.orden.creada", k,
                "{\"ordenId\":9003,\"usuarioOid\":\"oid\",\"items\":[{\"productoId\":1,\"nombre\":\"Item #falla\","
                        + "\"cantidad\":1,\"precioUnitario\":10.0}],\"total\":10.0,\"fecha\":\"2026-10-17T20:15:00Z\"}");
        ConsumerRecord<String, String> dlt = buscarEnDlt("nexus.orden.creada.DLT", k, Duration.ofSeconds(40));
        assertThat(dlt).as("mensaje en DLT").isNotNull();
        assertThat(header(dlt, "kafka_dlt-exception-cause-fqcn")).isEqualTo("java.lang.IllegalStateException");
        // 1 intento original + max-reintentos (3)
        assertThat(CONTADOR.intentos(k)).isEqualTo(4);
        assertThat(dlt.value()).contains("\"ordenId\":9003");
    }

    @Test
    void ordenSinItems_vaADlt() throws Exception {
        String k = key("ord-inv");
        enviar("nexus.orden.creada", k,
                "{\"ordenId\":9002,\"usuarioOid\":\"oid\",\"items\":[],\"total\":0.0,\"fecha\":\"2026-10-17T20:15:00Z\"}");
        ConsumerRecord<String, String> dlt = buscarEnDlt("nexus.orden.creada.DLT", k, Duration.ofSeconds(30));
        assertThat(dlt).isNotNull();
        assertThat(header(dlt, "kafka_dlt-exception-cause-fqcn")).endsWith("MensajeInvalidoException");
        assertThat(CONTADOR.intentos(k)).isEqualTo(1);
    }

    @Test
    void productoSinId_vaADlt_mismaParticion() throws Exception {
        String k = key("prd-inv");
        enviar("nexus.producto.eventos", k,
                "{\"productoId\":null,\"nombre\":\"Sin id\",\"stock\":1,\"accion\":\"CREADO\",\"fecha\":\"2026-10-17T20:15:00Z\"}");
        ConsumerRecord<String, String> dlt = buscarEnDlt("nexus.producto.eventos.DLT", k, Duration.ofSeconds(30));
        assertThat(dlt).isNotNull();
        assertThat(header(dlt, "kafka_dlt-exception-cause-fqcn")).endsWith("MensajeInvalidoException");
        // el recoverer publica en la misma particion del topic original
        assertThat(java.nio.ByteBuffer.wrap(dlt.headers().lastHeader("kafka_dlt-original-partition").value()).getInt())
                .isEqualTo(dlt.partition());
    }
}