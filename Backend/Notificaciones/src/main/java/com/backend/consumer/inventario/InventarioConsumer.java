package com.backend.consumer.inventario;

import com.backend.consumer.soporte.MensajeInvalidoException;
import com.backend.consumer.soporte.ProcesadorConAck;
import com.backend.messaging.event.ProductoEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Caso 3: auditoría de inventario y alerta de stock bajo (cola nexus.inventario.queue, binding producto.#). */
@Component
public class InventarioConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventarioConsumer.class);
    private final ProcesadorConAck procesador;

    public InventarioConsumer(ProcesadorConAck procesador) {
        this.procesador = procesador;
    }

    @RabbitListener(id = "inventario-listener", queues = "${app.messaging.queues.inventario}")
    public void recibir(ProductoEvent evento, Message mensaje, Channel channel) throws IOException {
        procesador.procesar(mensaje, channel, () -> {
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
        });
    }
}
