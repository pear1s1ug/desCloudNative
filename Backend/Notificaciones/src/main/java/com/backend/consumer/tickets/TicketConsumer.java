package com.backend.consumer.tickets;

import com.backend.consumer.soporte.MensajeInvalidoException;
import com.backend.consumer.soporte.ProcesadorConAck;
import com.backend.messaging.event.OrdenCreadaEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Caso 2: generación del ticket de compra (cola nexus.tickets.queue). */
@Component
public class TicketConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketConsumer.class);
    private final ProcesadorConAck procesador;

    public TicketConsumer(ProcesadorConAck procesador) {
        this.procesador = procesador;
    }

    @RabbitListener(id = "tickets-listener", queues = "${app.messaging.queues.tickets}")
    public void recibir(OrdenCreadaEvent evento, Message mensaje, Channel channel) throws IOException {
        procesador.procesar(mensaje, channel, () -> {
            if (evento.ordenId() == null || evento.items() == null || evento.items().isEmpty()
                    || evento.total() == null || evento.total() <= 0) {
                throw new MensajeInvalidoException("Orden sin productos o con total inválido");
            }
            if (evento.items().stream().anyMatch(i -> i.nombre() != null && i.nombre().contains("#falla"))) {
                throw new IllegalStateException("Simulación de falla al generar el ticket");
            }
            log.info("Ticket generado para la orden #{}: {} producto(s), total ${}",
                    evento.ordenId(), evento.items().size(), evento.total());
            evento.items().forEach(i -> log.info("   - {} x{} (${} c/u)", i.nombre(), i.cantidad(), i.precioUnitario()));
        });
    }
}
