package com.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ==========================================
 * TOPOLOGÍA DE MENSAJERÍA (la declara el consumidor)
 * ==========================================
 * 3 exchanges, 3 colas (con DLX y TTL), 3 DLQ y 6 bindings, agrupados por caso de uso.
 * Todos los nombres salen de app.messaging (MensajeriaProperties).
 *
 *   Caso 1  nexus.direct --usuario.registrado--> nexus.notificaciones.queue  (DLQ: nexus.notificaciones.dlq)
 *   Caso 2  nexus.direct --orden.creada-------> nexus.tickets.queue          (DLQ: nexus.tickets.dlq)
 *   Caso 3  nexus.topic  --producto.#---------> nexus.inventario.queue       (DLQ: nexus.inventario.dlq)
 *
 * Mensaje rechazado (NACK sin requeue) o expirado (TTL) -> nexus.dlx -> su DLQ.
 */
@Configuration
@EnableConfigurationProperties(MensajeriaProperties.class)
public class RabbitConfig {

    private final MensajeriaProperties props;

    public RabbitConfig(MensajeriaProperties props) {
        this.props = props;
    }

    // ===================== EXCHANGES =====================

    @Bean
    public DirectExchange directExchange() {
        return ExchangeBuilder.directExchange(props.exchanges().direct()).durable(true).build();
    }

    @Bean
    public TopicExchange topicExchange() {
        return ExchangeBuilder.topicExchange(props.exchanges().topic()).durable(true).build();
    }

    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(props.exchanges().dlx()).durable(true).build();
    }

    // ===================== CASO 1: BIENVENIDA =====================

    @Bean
    public Queue notificacionesQueue() {
        return colaPrincipal(props.queues().notificaciones(), props.dlq().notificaciones());
    }

    @Bean
    public Queue notificacionesDlq() {
        return dlq(props.dlq().notificaciones());
    }

    @Bean
    public Binding notificacionesBinding() {
        return BindingBuilder.bind(notificacionesQueue()).to(directExchange()).with(props.routingKeys().usuarioRegistrado());
    }

    @Bean
    public Binding notificacionesDlqBinding() {
        return BindingBuilder.bind(notificacionesDlq()).to(dlxExchange()).with(props.dlq().notificaciones());
    }

    // ===================== CASO 2: TICKETS =====================

    @Bean
    public Queue ticketsQueue() {
        return colaPrincipal(props.queues().tickets(), props.dlq().tickets());
    }

    @Bean
    public Queue ticketsDlq() {
        return dlq(props.dlq().tickets());
    }

    @Bean
    public Binding ticketsBinding() {
        return BindingBuilder.bind(ticketsQueue()).to(directExchange()).with(props.routingKeys().ordenCreada());
    }

    @Bean
    public Binding ticketsDlqBinding() {
        return BindingBuilder.bind(ticketsDlq()).to(dlxExchange()).with(props.dlq().tickets());
    }

    // ===================== CASO 3: INVENTARIO =====================

    @Bean
    public Queue inventarioQueue() {
        return colaPrincipal(props.queues().inventario(), props.dlq().inventario());
    }

    @Bean
    public Queue inventarioDlq() {
        return dlq(props.dlq().inventario());
    }

    @Bean
    public Binding inventarioBinding() {
        return BindingBuilder.bind(inventarioQueue()).to(topicExchange()).with(props.routingKeys().productoPatron());
    }

    @Bean
    public Binding inventarioDlqBinding() {
        return BindingBuilder.bind(inventarioDlq()).to(dlxExchange()).with(props.dlq().inventario());
    }

    // ===================== CONVERSOR JSON =====================

    /** Mismo conversor que los productores (Jackson 3): los eventos viajan según infra/eventos.md. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    // ===================== AUXILIARES =====================

    /** Cola principal: durable, con dead lettering hacia su DLQ y retención (TTL) de app.messaging.ttl-ms. */
    private Queue colaPrincipal(String nombre, String dlq) {
        return QueueBuilder.durable(nombre)
                .deadLetterExchange(props.exchanges().dlx())
                .deadLetterRoutingKey(dlq)
                .ttl((int) props.ttlMs())
                .build();
    }

    /** DLQ: durable, con retención de app.messaging.dlq-ttl-ms (24 h) para analizar los fallos. */
    private Queue dlq(String nombre) {
        return QueueBuilder.durable(nombre)
                .ttl((int) props.dlqTtlMs())
                .build();
    }
}
