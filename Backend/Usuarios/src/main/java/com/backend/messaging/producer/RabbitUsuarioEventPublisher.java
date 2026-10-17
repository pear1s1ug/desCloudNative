package com.backend.messaging.producer;

import com.backend.config.MensajeriaProperties;
import com.backend.messaging.event.UsuarioRegistradoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RabbitUsuarioEventPublisher implements UsuarioEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitUsuarioEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public RabbitUsuarioEventPublisher(RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @Override
    public void usuarioRegistrado(String azureOid, String nombre, String email) {
        UsuarioRegistradoEvent evento = new UsuarioRegistradoEvent(azureOid, nombre, email, Instant.now());
        rabbitTemplate.convertAndSend(props.exchanges().direct(), props.routingKeys().usuarioRegistrado(), evento);
        log.info("Evento publicado en {} con routing key {}: {}",
                props.exchanges().direct(), props.routingKeys().usuarioRegistrado(), evento);
    }
}