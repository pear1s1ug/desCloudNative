package com.backend.consumer.notificaciones;

import com.backend.consumer.soporte.MensajeInvalidoException;
import com.backend.messaging.event.UsuarioRegistradoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Caso 1: notificación de bienvenida a usuarios nuevos (topic nexus.usuario.registrado). */
@Component
public class BienvenidaConsumer {

    private static final Logger log = LoggerFactory.getLogger(BienvenidaConsumer.class);

    @KafkaListener(
            id = "bienvenida-listener",
            topics = "${app.messaging.topics.usuario-registrado}",
            groupId = "${app.messaging.consumer-groups.notificaciones}",
            properties = {
                    "spring.json.value.default.type=com.backend.messaging.event.UsuarioRegistradoEvent"
            })
    public void recibir(UsuarioRegistradoEvent evento) {
        if (evento.email() == null || evento.email().isBlank()) {
            throw new MensajeInvalidoException("Usuario sin email: no se puede notificar");
        }
        if (evento.nombre() != null && evento.nombre().contains("#falla")) {
            throw new IllegalStateException("Simulación de falla del servidor de correo");
        }
        log.info("Correo de bienvenida enviado a {} ({})", evento.nombre(), evento.email());
    }
}
