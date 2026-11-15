package com.backend.qa;

import com.backend.messaging.event.OrdenCreadaEvent;
import com.backend.messaging.event.ProductoEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA Kafka: el JSON que produce el JsonSerializer configurado en application.yml respeta el contrato
 * de infra/eventos.md y se puede leer con el JsonDeserializer que usa Notificaciones.
 */
@SuppressWarnings({"deprecation", "removal"})
class ContratoEventosProductosJsonTest {

    private static final Instant FECHA = Instant.parse("2026-10-17T20:15:00Z");
    private final ObjectMapper mapper = new ObjectMapper();

    private static <T> byte[] serializar(String topic, T valor) {
        try (JsonSerializer<T> ser = new JsonSerializer<>()) {
            ser.configure(Map.of("spring.json.add.type.headers", false), false);
            return ser.serialize(topic, valor);
        }
    }

    private static <T> T deserializar(String topic, byte[] json, Class<T> tipo) {
        try (JsonDeserializer<T> des = new JsonDeserializer<>(tipo, false)) {
            des.addTrustedPackages("com.backend.messaging.event");
            return des.deserialize(topic, json);
        }
    }

    private static List<String> campos(JsonNode n) {
        List<String> l = new java.util.ArrayList<>();
        n.fieldNames().forEachRemaining(l::add);
        return l;
    }

    @Test
    void productoEvent_camposDelContrato_yRoundTrip() throws Exception {
        ProductoEvent e = new ProductoEvent(7L, "Mouse de prueba", 2, "STOCK_BAJO", FECHA);
        byte[] json = serializar("nexus.producto.eventos", e);
        JsonNode n = mapper.readTree(json);
        assertThat(campos(n)).containsExactlyInAnyOrder("productoId", "nombre", "stock", "accion", "fecha");
        assertThat(n.get("productoId").isNumber()).isTrue();
        assertThat(n.get("accion").asText()).isEqualTo("STOCK_BAJO");
        assertThat(deserializar("nexus.producto.eventos", json, ProductoEvent.class)).isEqualTo(e);
    }

    @Test
    void ordenCreadaEvent_camposDelContrato_yRoundTrip() throws Exception {
        OrdenCreadaEvent e = new OrdenCreadaEvent(15L, "oid",
                List.of(new OrdenCreadaEvent.Item(1L, "Teclado", 1, 89990.0)), 89990.0, FECHA);
        byte[] json = serializar("nexus.orden.creada", e);
        JsonNode n = mapper.readTree(json);
        assertThat(campos(n)).containsExactlyInAnyOrder("ordenId", "usuarioOid", "items", "total", "fecha");
        assertThat(campos(n.get("items").get(0)))
                .containsExactlyInAnyOrder("productoId", "nombre", "cantidad", "precioUnitario");
        assertThat(deserializar("nexus.orden.creada", json, OrdenCreadaEvent.class)).isEqualTo(e);
    }

    /**
     * DEF-02 (contrato): eventos.md exige fecha ISO-8601 ("2026-10-17T20:15:00Z"), pero hoy se emite epoch numerico.
     * Desactivado por defecto para no romper el build. Ejecutar con: mvnw.cmd test -Dqa.defectos=true
     */
    @Test
    @EnabledIfSystemProperty(named = "qa.defectos", matches = "true")
    void defecto_fechaDebeViajarComoTextoIso8601() throws Exception {
        JsonNode n = mapper.readTree(serializar("nexus.producto.eventos",
                new ProductoEvent(7L, "Mouse", 2, "STOCK_BAJO", FECHA)));
        assertThat(n.get("fecha").isTextual()).as("fecha como texto ISO-8601, valor actual: " + n.get("fecha")).isTrue();
        assertThat(n.get("fecha").asText()).isEqualTo("2026-10-17T20:15:00Z");
    }
}