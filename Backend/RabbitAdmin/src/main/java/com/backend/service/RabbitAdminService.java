package com.backend.service;

import com.backend.config.MensajeriaProperties;
import com.backend.dto.ColaResponse;
import com.backend.dto.CrearBindingRequest;
import com.backend.dto.CrearColaRequest;
import com.backend.dto.CrearExchangeRequest;
import com.backend.exception.ConflictoException;
import com.backend.exception.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * ==========================================
 * CAPA DE SERVICIO (ADMINISTRACIÓN DE RABBITMQ)
 * ==========================================
 * Única clase que habla con la API de administración de RabbitMQ (AmqpAdmin).
 * El controlador solo llama a estos métodos de alto nivel.
 *
 * Reglas:
 * - Nombres que empiezan con "amq." están reservados por RabbitMQ -> 409.
 * - Las colas, DLQ y exchanges del sistema (app.messaging) no se pueden borrar ni modificar -> 409.
 * - Borrar o enlazar un recurso inexistente -> 404.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);
    private static final String PREFIJO_RESERVADO = "amq.";

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;
    private final Set<String> recursosDelSistema;

    public RabbitAdminService(AmqpAdmin amqpAdmin, RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.amqpAdmin = amqpAdmin;
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
        this.recursosDelSistema = Set.of(
                props.queues().notificaciones(), props.queues().tickets(), props.queues().inventario(),
                props.dlq().notificaciones(), props.dlq().tickets(), props.dlq().inventario(),
                props.exchanges().direct(), props.exchanges().topic(), props.exchanges().dlx());
    }

    // ===================== COLAS =====================

    public ColaResponse crearCola(CrearColaRequest req) {
        validarNombreLibre(req.nombre());
        if (amqpAdmin.getQueueInfo(req.nombre()) != null) {
            throw new ConflictoException("La cola '" + req.nombre() + "' ya existe");
        }

        boolean durable = req.durable() == null || req.durable();
        QueueBuilder builder = durable ? QueueBuilder.durable(req.nombre()) : QueueBuilder.nonDurable(req.nombre());

        if (Boolean.TRUE.equals(req.conDlq())) {
            String dlq = req.nombre() + ".dlq";
            String dlx = props.exchanges().dlx();
            amqpAdmin.declareExchange(new DirectExchange(dlx, true, false));
            Queue colaDlq = QueueBuilder.durable(dlq).build();
            amqpAdmin.declareQueue(colaDlq);
            amqpAdmin.declareBinding(new Binding(dlq, Binding.DestinationType.QUEUE, dlx, dlq, null));
            builder.deadLetterExchange(dlx).deadLetterRoutingKey(dlq);
        }
        if (req.ttlMs() != null) {
            builder.ttl(req.ttlMs().intValue());
        }

        amqpAdmin.declareQueue(builder.build());
        log.info("Cola creada: {} (durable={}, conDlq={}, ttlMs={})", req.nombre(), durable, req.conDlq(), req.ttlMs());
        return obtenerCola(req.nombre());
    }

    public ColaResponse obtenerCola(String nombre) {
        QueueInformation info = amqpAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        return new ColaResponse(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    public void eliminarCola(String nombre) {
        validarNoEsDelSistema(nombre);
        obtenerCola(nombre); // 404 si no existe
        amqpAdmin.deleteQueue(nombre);
        log.info("Cola eliminada: {}", nombre);
    }

    public int purgarCola(String nombre) {
        obtenerCola(nombre); // 404 si no existe
        int eliminados = amqpAdmin.purgeQueue(nombre);
        log.info("Cola purgada: {} ({} mensajes eliminados)", nombre, eliminados);
        return eliminados;
    }

    // ===================== EXCHANGES =====================

    public void crearExchange(CrearExchangeRequest req) {
        validarNombreLibre(req.nombre());
        if (existeExchange(req.nombre())) {
            throw new ConflictoException("El exchange '" + req.nombre() + "' ya existe");
        }

        boolean durable = req.durable() == null || req.durable();
        Exchange exchange = switch (req.tipo()) {
            case DIRECT -> ExchangeBuilder.directExchange(req.nombre()).durable(durable).build();
            case TOPIC -> ExchangeBuilder.topicExchange(req.nombre()).durable(durable).build();
            case FANOUT -> ExchangeBuilder.fanoutExchange(req.nombre()).durable(durable).build();
        };

        amqpAdmin.declareExchange(exchange);
        log.info("Exchange creado: {} (tipo={}, durable={})", req.nombre(), req.tipo(), durable);
    }

    public void eliminarExchange(String nombre) {
        validarNoEsDelSistema(nombre);
        if (!existeExchange(nombre)) {
            throw new RecursoNoEncontradoException("El exchange '" + nombre + "' no existe");
        }
        amqpAdmin.deleteExchange(nombre);
        log.info("Exchange eliminado: {}", nombre);
    }

    // ===================== BINDINGS =====================

    public void crearBinding(CrearBindingRequest req) {
        validarNoEsDelSistema(req.cola());
        validarQueExisten(req.cola(), req.exchange());
        amqpAdmin.declareBinding(new Binding(req.cola(), Binding.DestinationType.QUEUE, req.exchange(), req.routingKey(), null));
        log.info("Binding creado: {} -> {} ({})", req.exchange(), req.cola(), req.routingKey());
    }

    public void eliminarBinding(String cola, String exchange, String routingKey) {
        validarNoEsDelSistema(cola);
        validarQueExisten(cola, exchange);
        amqpAdmin.removeBinding(new Binding(cola, Binding.DestinationType.QUEUE, exchange, routingKey, null));
        log.info("Binding eliminado: {} -> {} ({})", exchange, cola, routingKey);
    }

    // ===================== REGLAS =====================

    private void validarNombreLibre(String nombre) {
        if (nombre.startsWith(PREFIJO_RESERVADO)) {
            throw new ConflictoException("Los nombres que empiezan con 'amq.' están reservados por RabbitMQ");
        }
        validarNoEsDelSistema(nombre);
    }

    private void validarNoEsDelSistema(String nombre) {
        if (recursosDelSistema.contains(nombre)) {
            throw new ConflictoException("'" + nombre + "' es un recurso del sistema y no se puede modificar desde la API");
        }
    }

    private void validarQueExisten(String cola, String exchange) {
        obtenerCola(cola); // 404 si no existe
        if (!existeExchange(exchange)) {
            throw new RecursoNoEncontradoException("El exchange '" + exchange + "' no existe");
        }
    }

    /** Declaración pasiva: falla si el exchange no existe (AmqpAdmin no tiene un "getExchangeInfo"). */
    private boolean existeExchange(String nombre) {
        try {
            rabbitTemplate.execute(channel -> channel.exchangeDeclarePassive(nombre));
            return true;
        } catch (AmqpConnectException e) {
            throw e; // broker caído: lo traduce a 503 el manejador global
        } catch (AmqpException e) {
            return false;
        }
    }
}