package com.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(MensajeriaProperties.class)
public class RabbitConfig {

    private final MensajeriaProperties props;

    public RabbitConfig(MensajeriaProperties props) {
        this.props = props;
    }

    // ==========================================
    // 1. EXCHANGES (Direct, Topic, DLX)
    // ==========================================
    @Bean
    public DirectExchange nexusDirectExchange() {
        return new DirectExchange(props.exchangeDirect());
    }

    @Bean
    public TopicExchange nexusTopicExchange() {
        return new TopicExchange(props.exchangeTopic());
    }

    @Bean
    public DirectExchange nexusDlxExchange() {
        return new DirectExchange(props.exchangeDlx());
    }

    // ==========================================
    // 2. COLAS Y DLQ POR CASO DE USO
    // ==========================================

    // --- Caso 1: Bienvenida Usuarios ---
    @Bean
    public Queue usuarioBienvenidaQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", props.exchangeDlx());
        args.put("x-dead-letter-routing-key", "usuario.bienvenida.dlq");
        return new Queue("notificaciones.usuario.bienvenida", true, false, false, args);
    }

    @Bean
    public Queue usuarioBienvenidaDlq() {
        return new Queue("notificaciones.usuario.bienvenida.dlq", true);
    }

    // --- Caso 2: Tickets Soporte ---
    @Bean
    public Queue ticketSoporteQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", props.exchangeDlx());
        args.put("x-dead-letter-routing-key", "ticket.soporte.dlq");
        return new Queue("notificaciones.ticket.soporte", true, false, false, args);
    }

    @Bean
    public Queue ticketSoporteDlq() {
        return new Queue("notificaciones.ticket.soporte.dlq", true);
    }

    // --- Caso 3: Alerta Inventario ---
    @Bean
    public Queue inventarioAlertaQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", props.exchangeDlx());
        args.put("x-dead-letter-routing-key", "inventario.alerta.dlq");
        return new Queue("notificaciones.inventario.alerta", true, false, false, args);
    }

    @Bean
    public Queue inventarioAlertaDlq() {
        return new Queue("notificaciones.inventario.alerta.dlq", true);
    }

    // ==========================================
    // 3. BINDINGS (6 BINDINGS)
    // ==========================================
    @Bean
    public Binding bindingBienvenida(Queue usuarioBienvenidaQueue, TopicExchange nexusTopicExchange) {
        return BindingBuilder.bind(usuarioBienvenidaQueue).to(nexusTopicExchange).with("usuario.registrado");
    }

    @Bean
    public Binding bindingBienvenidaDlq(Queue usuarioBienvenidaDlq, DirectExchange nexusDlxExchange) {
        return BindingBuilder.bind(usuarioBienvenidaDlq).to(nexusDlxExchange).with("usuario.bienvenida.dlq");
    }

    @Bean
    public Binding bindingTicket(Queue ticketSoporteQueue, DirectExchange nexusDirectExchange) {
        return BindingBuilder.bind(ticketSoporteQueue).to(nexusDirectExchange).with("ticket.creado");
    }

    @Bean
    public Binding bindingTicketDlq(Queue ticketSoporteDlq, DirectExchange nexusDlxExchange) {
        return BindingBuilder.bind(ticketSoporteDlq).to(nexusDlxExchange).with("ticket.soporte.dlq");
    }

    @Bean
    public Binding bindingInventario(Queue inventarioAlertaQueue, TopicExchange nexusTopicExchange) {
        return BindingBuilder.bind(inventarioAlertaQueue).to(nexusTopicExchange).with("producto.stock.bajo");
    }

    @Bean
    public Binding bindingInventarioDlq(Queue inventarioAlertaDlq, DirectExchange nexusDlxExchange) {
        return BindingBuilder.bind(inventarioAlertaDlq).to(nexusDlxExchange).with("inventario.alerta.dlq");
    }

    // ==========================================
    // 4. CONVERSOR JSON
    // ==========================================
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
