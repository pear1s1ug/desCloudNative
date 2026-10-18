package com.backend.consumer.notificaciones;

import com.backend.consumer.soporte.MensajeInvalidoException;
import com.backend.consumer.soporte.ProcesadorConAck;
import com.backend.messaging.event.UsuarioRegistradoEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class BienvenidaConsumer {

    private static final Logger log = LoggerFactory.getLogger(BienvenidaConsumer.class);
    private final ProcesadorConAck procesador;

    public BienvenidaConsumer(ProcesadorConAck procesador) {
        this.procesador = procesador;
    }

    @RabbitListener(queues = "notificaciones.usuario.bienvenida")
    public void consumir(UsuarioRegistradoEvent event, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, Channel channel) throws IOException {
        procesador.procesar(() -> {
            if (event.email() == null || event.email().isBlank()) {
                throw new MensajeInvalidoException("Email vacio o nulo");
            }
            if (event.nombre() != null && event.nombre().contains("#falla")) {
                throw new RuntimeException("Simulacion de falla transitoria");
            }
            log.info("Notificacion de bienvenida enviada a: {}", event.email());
        }, deliveryTag, channel);
    }
}