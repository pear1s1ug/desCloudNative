package com.backend.consumer.inventario;

import com.backend.consumer.soporte.MensajeInvalidoException;
import com.backend.messaging.event.ProductoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Caso 3: auditoría de inventario y alerta de stock bajo (topic nexus.producto.eventos). */
@Component
public class InventarioConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventarioConsumer.class);

    @KafkaListener(
            id = "inventario-listener",
            topics = "${app.messaging.topics.producto-eventos}",
            groupId = "${app.messaging.consumer-groups.inventario}",
            properties = {
                    "spring.json.value.default.type=com.backend.messaging.event.ProductoEvent"
            })
    public void recibir(ProductoEvent evento) {
        if (evento.productoId() == null) {
            throw new MensajeInvalidoException("Evento de inventario sin productoId");
        }
        if (evento.nombre() != null && evento.nombre().contains("#falla")) {
            throw new IllegalStateException("Simulación de falla en el registro de auditoría");
        }
        if ("STOCK_BAJO".equals(evento.accion())) {
            log.warn("[ALERTA STOCK BAJO] {} (id {}) quedó con {} unidad(es)",
                    evento.nombre(), evento.productoId(), evento.stock());
        } else {
            log.info("Auditoría de inventario: producto {} (id {}) {}, stock {}",
                    evento.nombre(), evento.productoId(), evento.accion(), evento.stock());
        }
    }
}
