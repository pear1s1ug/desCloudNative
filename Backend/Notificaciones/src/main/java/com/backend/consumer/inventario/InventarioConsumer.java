package com.backend.consumer.inventario;

import com.backend.consumer.soporte.MensajeInvalidoException;
import com.backend.consumer.soporte.ProcesadorConAck;
import com.backend.messaging.event.StockBajoEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class InventarioConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventarioConsumer.class);
    private final ProcesadorConAck procesador;

    public InventarioConsumer(ProcesadorConAck procesador) {
        this.procesador = procesador;
    }

    @RabbitListener(queues = "notificaciones.inventario.alerta")
    public void consumir(StockBajoEvent event, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, Channel channel) throws IOException {
        procesador.procesar(() -> {
            if (event.productoId() == null) {
                throw new MensajeInvalidoException("ID de producto no informado");
            }
            if (event.nombre() != null && event.nombre().contains("#falla")) {
                throw new RuntimeException("Simulacion de falla en inventario");
            }
            log.info("Alerta de stock bajo para producto: {}. Quedan: {}", event.nombre(), event.stock());
        }, deliveryTag, channel);
    }
}