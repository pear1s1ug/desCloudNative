package com.backend.consumer.tickets;

import com.backend.consumer.soporte.MensajeInvalidoException;
import com.backend.consumer.soporte.ProcesadorConAck;
import com.backend.messaging.event.TicketCreadoEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TicketConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketConsumer.class);
    private final ProcesadorConAck procesador;

    public TicketConsumer(ProcesadorConAck procesador) {
        this.procesador = procesador;
    }

    @RabbitListener(queues = "notificaciones.ticket.soporte")
    public void consumir(TicketCreadoEvent event, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, Channel channel) throws IOException {
        procesador.procesar(() -> {
            if (event.asunto() == null || event.asunto().isBlank()) {
                throw new MensajeInvalidoException("Asunto del ticket nulo o vacio");
            }
            if (event.asunto().contains("#falla")) {
                throw new RuntimeException("Simulacion de falla en ticket");
            }
            log.info("Ticket de soporte registrado con exito: {}", event.asunto());
        }, deliveryTag, channel);
    }
}