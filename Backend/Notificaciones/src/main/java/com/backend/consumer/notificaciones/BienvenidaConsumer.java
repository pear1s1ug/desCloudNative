package com.backend.consumer.notificaciones;

import com.backend.consumer.soporte.MensajeInvalidoException;
import com.backend.consumer.soporte.ProcesadorConAck;
import com.backend.messaging.event.UsuarioRegistradoEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Caso 1: notificación de bienvenida a usuarios nuevos (cola nexus.notificaciones.queue). */
@Component
public class BienvenidaConsumer {

    private static final Logger log = LoggerFactory.getLogger(BienvenidaConsumer.class);
    private final ProcesadorConAck procesador;

    public BienvenidaConsumer(ProcesadorConAck procesador) {
        this.procesador = procesador;
    }

    @RabbitListener(id = "bienvenida-listener", queues = "${app.messaging.queues.notificaciones}")
    public void recibir(UsuarioRegistradoEvent evento, Message mensaje, Channel channel) throws IOException {
        procesador.procesar(mensaje, channel, () -> {
            if (evento.email() == null || evento.email().isBlank()) {
                throw new MensajeInvalidoException("Usuario sin email: no se puede notificar");
            }
            if (evento.nombre() != null && evento.nombre().contains("#falla")) {
                throw new IllegalStateException("Simulación de falla del servidor de correo");
            }
            log.info("Correo de bienvenida enviado a {} ({})", evento.nombre(), evento.email());
        });
    }
}
