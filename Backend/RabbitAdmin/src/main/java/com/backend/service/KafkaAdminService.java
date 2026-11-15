package com.backend.service;

import com.backend.config.MensajeriaProperties;
import com.backend.dto.CrearTopicRequest;
import com.backend.dto.TopicResponse;
import com.backend.exception.ConflictoException;
import com.backend.exception.RecursoNoEncontradoException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Administración de topics Kafka vía AdminClient.
 * Protege los topics del sistema (app.messaging) y los nombres internos (__*).
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);
    private static final String PREFIJO_RESERVADO = "__";
    private static final long TIMEOUT_SEG = 15;

    private final AdminClient adminClient;
    private final Set<String> topicsDelSistema;

    public KafkaAdminService(AdminClient adminClient, MensajeriaProperties props) {
        this.adminClient = adminClient;
        this.topicsDelSistema = Set.of(
                props.topics().usuarioRegistrado(),
                props.topics().ordenCreada(),
                props.topics().productoEventos(),
                props.dlt().usuarioRegistrado(),
                props.dlt().ordenCreada(),
                props.dlt().productoEventos());
    }

    public TopicResponse crearTopic(CrearTopicRequest req) {
        validarNombreLibre(req.nombre());
        if (existeTopic(req.nombre())) {
            throw new ConflictoException("El topic '" + req.nombre() + "' ya existe");
        }

        int partitions = req.partitions() == null ? 1 : req.partitions();
        short replication = req.replicationFactor() == null ? 1 : req.replicationFactor();
        NewTopic topic = new NewTopic(req.nombre(), partitions, replication);

        try {
            adminClient.createTopics(List.of(topic)).all().get(TIMEOUT_SEG, TimeUnit.SECONDS);
            log.info("Topic creado: {} (partitions={}, replicationFactor={})", req.nombre(), partitions, replication);
            return obtenerTopic(req.nombre());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TopicExistsException) {
                throw new ConflictoException("El topic '" + req.nombre() + "' ya existe");
            }
            throw traducirError(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación interrumpida", e);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Timeout al crear el topic en Kafka", e);
        }
    }

    public TopicResponse obtenerTopic(String nombre) {
        try {
            Map<String, TopicDescription> descriptions = adminClient.describeTopics(List.of(nombre))
                    .allTopicNames()
                    .get(TIMEOUT_SEG, TimeUnit.SECONDS);
            TopicDescription description = descriptions.get(nombre);
            if (description == null) {
                throw new RecursoNoEncontradoException("El topic '" + nombre + "' no existe");
            }
            int partitions = description.partitions().size();
            int replication = description.partitions().isEmpty()
                    ? 0
                    : description.partitions().get(0).replicas().size();
            return new TopicResponse(nombre, partitions, replication);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                throw new RecursoNoEncontradoException("El topic '" + nombre + "' no existe");
            }
            throw traducirError(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación interrumpida", e);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Timeout al consultar el topic en Kafka", e);
        }
    }

    public void eliminarTopic(String nombre) {
        validarNoEsDelSistema(nombre);
        obtenerTopic(nombre); // 404 si no existe
        try {
            adminClient.deleteTopics(List.of(nombre)).all().get(TIMEOUT_SEG, TimeUnit.SECONDS);
            log.info("Topic eliminado: {}", nombre);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                throw new RecursoNoEncontradoException("El topic '" + nombre + "' no existe");
            }
            throw traducirError(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación interrumpida", e);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Timeout al eliminar el topic en Kafka", e);
        }
    }

    private boolean existeTopic(String nombre) {
        try {
            Collection<String> names = adminClient.listTopics().names().get(TIMEOUT_SEG, TimeUnit.SECONDS);
            return names.contains(nombre);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación interrumpida", e);
        } catch (ExecutionException | TimeoutException e) {
            throw traducirError(e);
        }
    }

    private void validarNombreLibre(String nombre) {
        if (nombre.startsWith(PREFIJO_RESERVADO)) {
            throw new ConflictoException("Los nombres que empiezan con '__' están reservados por Kafka");
        }
        validarNoEsDelSistema(nombre);
    }

    private void validarNoEsDelSistema(String nombre) {
        if (topicsDelSistema.contains(nombre)) {
            throw new ConflictoException("'" + nombre + "' es un topic del sistema y no se puede modificar desde la API");
        }
    }

    private RuntimeException traducirError(Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        return new org.apache.kafka.common.KafkaException(
                "Kafka rechazó la operación: " + cause.getMessage(), cause);
    }
}
