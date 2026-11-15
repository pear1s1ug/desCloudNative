package com.backend.qa;

import com.backend.dto.CrearTopicRequest;
import com.backend.dto.TopicResponse;
import com.backend.exception.ConflictoException;
import com.backend.exception.RecursoNoEncontradoException;
import com.backend.service.KafkaAdminService;
import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/** QA Kafka: KafkaAdminService contra un broker embebido real (crear, consultar, borrar, protecciones). */
@SpringBootTest
@EmbeddedKafka(partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers",
        topics = {"nexus.orden.creada", "nexus.orden.creada.DLT"})
@DirtiesContext
class KafkaAdminServiceEmbeddedTest {

    @Autowired
    private KafkaAdminService service;

    @Autowired
    private AdminClient adminClient;

    private Set<String> topics() throws Exception {
        return adminClient.listTopics().names().get(10, TimeUnit.SECONDS);
    }

    @Test
    void crearConsultarYBorrarTopic() throws Exception {
        TopicResponse creado = service.crearTopic(new CrearTopicRequest("qa.admin.topic", 2, (short) 1));
        assertThat(creado).isEqualTo(new TopicResponse("qa.admin.topic", 2, 1));
        assertThat(service.obtenerTopic("qa.admin.topic").partitions()).isEqualTo(2);

        service.eliminarTopic("qa.admin.topic");
        await().atMost(java.time.Duration.ofSeconds(15)).until(() -> !topics().contains("qa.admin.topic"));
    }

    @Test
    void crearTopicDuplicado_409() {
        service.crearTopic(new CrearTopicRequest("qa.dup.topic", 1, (short) 1));
        assertThatThrownBy(() -> service.crearTopic(new CrearTopicRequest("qa.dup.topic", 1, (short) 1)))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void valoresPorDefecto_1particion_rf1() {
        TopicResponse r = service.crearTopic(new CrearTopicRequest("qa.default.topic", null, null));
        assertThat(r.partitions()).isEqualTo(1);
        assertThat(r.replicationFactor()).isEqualTo(1);
    }

    @Test
    void replicationFactorMayorQueBrokers_noEs2xx() {
        // 1 broker: RF 3 debe ser rechazado por Kafka (el controller lo traduce a 502)
        assertThatThrownBy(() -> service.crearTopic(new CrearTopicRequest("qa.rf.topic", 1, (short) 3)))
                .isInstanceOf(org.apache.kafka.common.KafkaException.class);
    }

    @Test
    void topicInexistente_404() {
        assertThatThrownBy(() -> service.obtenerTopic("no.existe"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> service.eliminarTopic("no.existe"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void topicsDelSistema_noSePuedenCrearNiBorrar_409() throws Exception {
        assertThatThrownBy(() -> service.eliminarTopic("nexus.orden.creada")).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> service.eliminarTopic("nexus.orden.creada.DLT")).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> service.crearTopic(new CrearTopicRequest("nexus.usuario.registrado", 1, (short) 1)))
                .isInstanceOf(ConflictoException.class);
        assertThat(topics()).contains("nexus.orden.creada", "nexus.orden.creada.DLT");
    }

    @Test
    void prefijoReservado_noSePuedeCrear_409() {
        assertThatThrownBy(() -> service.crearTopic(new CrearTopicRequest("__qa", 1, (short) 1)))
                .isInstanceOf(ConflictoException.class);
    }

    /**
     * DEF-03: eliminarTopic no bloquea el prefijo reservado "__" (solo crearTopic lo hace) y la regex de la ruta lo permite.
     * Se espera 409 sin llegar a Kafka. Ejecutar con: mvnw.cmd test -Dqa.defectos=true
     */
    @Test
    @EnabledIfSystemProperty(named = "qa.defectos", matches = "true")
    void defecto_eliminarTopicInterno_debeSer409() {
        assertThatThrownBy(() -> service.eliminarTopic("__consumer_offsets")).isInstanceOf(ConflictoException.class);
    }
}