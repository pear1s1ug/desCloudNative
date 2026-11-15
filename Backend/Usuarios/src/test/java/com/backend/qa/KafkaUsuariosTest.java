package com.backend.qa;

import com.backend.config.MensajeriaProperties;
import com.backend.messaging.event.UsuarioRegistradoEvent;
import com.backend.messaging.producer.KafkaUsuarioEventPublisher;
import com.backend.messaging.producer.UsuarioEventPublisher;
import com.backend.model.UsuarioModel;
import com.backend.repository.UsuarioRepository;
import com.backend.service.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** QA Kafka del servicio Usuarios: publisher, regla "solo usuarios nuevos" y contrato JSON. Sin broker ni BD. */
@SuppressWarnings({"unchecked", "deprecation", "removal"})
class KafkaUsuariosTest {

    private static final Instant FECHA = Instant.parse("2026-10-17T20:15:00Z");

    static MensajeriaProperties props() {
        return new MensajeriaProperties(
                new MensajeriaProperties.Topics("nexus.usuario.registrado", "nexus.orden.creada", "nexus.producto.eventos"),
                new MensajeriaProperties.Dlt("nexus.usuario.registrado.DLT", "nexus.orden.creada.DLT", "nexus.producto.eventos.DLT"),
                new MensajeriaProperties.ConsumerGroups("nexus.notificaciones", "nexus.tickets", "nexus.inventario"),
                3, (short) 1, 3);
    }

    @Test
    void publisher_usaTopicYKeyAzureOid() {
        KafkaTemplate<String, UsuarioRegistradoEvent> template = mock(KafkaTemplate.class);
        new KafkaUsuarioEventPublisher(template, props()).usuarioRegistrado("oid-1", "Ana", "ana@test.cl");

        ArgumentCaptor<UsuarioRegistradoEvent> ev = ArgumentCaptor.forClass(UsuarioRegistradoEvent.class);
        verify(template).send(eq("nexus.usuario.registrado"), eq("oid-1"), ev.capture());
        assertThat(ev.getValue().azureOid()).isEqualTo("oid-1");
        assertThat(ev.getValue().nombre()).isEqualTo("Ana");
        assertThat(ev.getValue().email()).isEqualTo("ana@test.cl");
        assertThat(ev.getValue().fecha()).isNotNull();
    }

    @Test
    void sincronizar_usuarioNuevo_publicaEvento() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        UsuarioEventPublisher pub = mock(UsuarioEventPublisher.class);
        when(repo.findByAzureOid("oid-1")).thenReturn(Optional.empty());
        when(repo.save(any(UsuarioModel.class))).thenAnswer(inv -> {
            UsuarioModel u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        new UsuarioService(repo, pub).sincronizar("oid-1", "Ana", "ana@test.cl", "Cliente");
        verify(pub).usuarioRegistrado("oid-1", "Ana", "ana@test.cl");
    }

    @Test
    void sincronizar_usuarioExistente_noPublica() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        UsuarioEventPublisher pub = mock(UsuarioEventPublisher.class);
        UsuarioModel existente = UsuarioModel.builder().azureOid("oid-1").build();
        existente.setId(5L);
        when(repo.findByAzureOid("oid-1")).thenReturn(Optional.of(existente));
        when(repo.save(any(UsuarioModel.class))).thenAnswer(inv -> inv.getArgument(0));
        new UsuarioService(repo, pub).sincronizar("oid-1", "Ana", "ana@test.cl", "Cliente");
        verify(pub, never()).usuarioRegistrado(anyString(), anyString(), anyString());
    }

    private static byte[] serializar(UsuarioRegistradoEvent e) {
        try (JsonSerializer<UsuarioRegistradoEvent> ser = new JsonSerializer<>()) {
            ser.configure(Map.of("spring.json.add.type.headers", false), false);
            return ser.serialize("nexus.usuario.registrado", e);
        }
    }

    @Test
    void contratoJson_camposYRoundTrip() throws Exception {
        UsuarioRegistradoEvent e = new UsuarioRegistradoEvent("oid-1", "Ana Perez", "ana@test.cl", FECHA);
        byte[] json = serializar(e);
        JsonNode n = new ObjectMapper().readTree(json);
        List<String> campos = new ArrayList<>();
        n.fieldNames().forEachRemaining(campos::add);
        assertThat(campos).containsExactlyInAnyOrder("azureOid", "nombre", "email", "fecha");
        try (JsonDeserializer<UsuarioRegistradoEvent> des = new JsonDeserializer<>(UsuarioRegistradoEvent.class, false)) {
            assertThat(des.deserialize("nexus.usuario.registrado", json)).isEqualTo(e);
        }
    }

    /** DEF-02: fecha debe viajar como ISO-8601. Ejecutar con: mvnw.cmd test -Dqa.defectos=true */
    @Test
    @EnabledIfSystemProperty(named = "qa.defectos", matches = "true")
    void defecto_fechaDebeViajarComoTextoIso8601() throws Exception {
        JsonNode n = new ObjectMapper().readTree(serializar(new UsuarioRegistradoEvent("o", "n", "e@x.cl", FECHA)));
        assertThat(n.get("fecha").isTextual()).as("valor actual: " + n.get("fecha")).isTrue();
    }
}