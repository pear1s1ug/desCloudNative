package com.backend.consumer.tickets;

import com.backend.consumer.soporte.MensajeInvalidoException;
import com.backend.messaging.event.OrdenCreadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Caso 2: generación del ticket de compra (topic nexus.orden.creada). */
@Component
public class TicketConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketConsumer.class);

    @KafkaListener(
            id = "tickets-listener",
            topics = "${app.messaging.topics.orden-creada}",
            groupId = "${app.messaging.consumer-groups.tickets}",
            properties = {
                    "spring.json.value.default.type=com.backend.messaging.event.OrdenCreadaEvent"
            })
    public void recibir(OrdenCreadaEvent evento) {
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
    }
}
