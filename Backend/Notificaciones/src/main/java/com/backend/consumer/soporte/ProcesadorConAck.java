package com.backend.consumer.soporte;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
 * TABLA DE DECISIONES DE ACK / NACK:
 * --------------------------------------------------------------------------------------
 * Condición                             | Acción               | Requeue | Destino
 * --------------------------------------------------------------------------------------
 * Procesamiento Exitoso                 | channel.basicAck()   | N/A     | Consumido ok
 * MensajeInvalidoException (Dato Nulo)  | channel.basicNack()  | false   | DLQ directo
 * Error Transitorio (#falla)            | channel.basicNack()  | false   | DLQ para análisis
 * --------------------------------------------------------------------------------------
 */
@Component
public class ProcesadorConAck {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorConAck.class);

    @FunctionalInterface
    public interface AccionProcesamiento {
        void ejecutar() throws Exception;
    }

    public void procesar(AccionProcesamiento accion, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, Channel channel) throws IOException {
        try {
            accion.ejecutar();
            channel.basicAck(deliveryTag, false);
            log.info("ACK procesado correctamente. deliveryTag: {}", deliveryTag);
        } catch (MensajeInvalidoException e) {
            log.error("Dato inválido detectado. Redirigiendo a DLQ sin requeue. Mensaje: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("Error en procesamiento. Redirigiendo a DLQ. Error: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
}