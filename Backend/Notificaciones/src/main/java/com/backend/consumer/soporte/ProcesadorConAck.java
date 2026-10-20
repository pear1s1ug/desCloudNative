package com.backend.consumer.soporte;

import com.backend.config.MensajeriaProperties;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
 * TABLA DE DECISIONES DE ACK / NACK (acknowledge-mode: manual):
 * ------------------------------------------------------------------------------------------------
 * Condición                                  | Acción                                 | Destino
 * ------------------------------------------------------------------------------------------------
 * Procesamiento exitoso                      | basicAck                               | Consumido
 * MensajeInvalidoException (dato inválido)   | basicNack(requeue = false)             | DLQ directo
 * Error transitorio, intentos < máximo       | reenvía con x-reintentos + 1 y basicAck| Misma cola
 * Error transitorio, intentos agotados       | basicNack(requeue = false)             | DLQ
 * JSON que no se puede convertir             | el contenedor rechaza sin reencolar    | DLQ
 * ------------------------------------------------------------------------------------------------
 * El reintento se publica directo a la cola de origen (exchange por defecto), así no se
 * duplica en otras colas enlazadas al mismo exchange. Máximo: app.messaging.max-reintentos.
 */
@Component
public class ProcesadorConAck {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorConAck.class);
    static final String HEADER_REINTENTOS = "x-reintentos";

    @FunctionalInterface
    public interface AccionProcesamiento {
        void ejecutar() throws Exception;
    }

    private final RabbitTemplate rabbitTemplate;
    private final int maxReintentos;

    public ProcesadorConAck(RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.maxReintentos = props.maxReintentos();
    }

    public void procesar(Message mensaje, Channel channel, AccionProcesamiento accion) throws IOException {
        MessageProperties propiedades = mensaje.getMessageProperties();
        long deliveryTag = propiedades.getDeliveryTag();
        String cola = propiedades.getConsumerQueue();
        int intento = intentosPrevios(propiedades);

        try {
            accion.ejecutar();
            channel.basicAck(deliveryTag, false);
            log.info("ACK en {} (reintentos previos: {})", cola, intento);
        } catch (MensajeInvalidoException e) {
            log.error("Mensaje inválido en {}, se envía a la DLQ sin reintentar: {}", cola, e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            if (intento < maxReintentos) {
                propiedades.setHeader(HEADER_REINTENTOS, intento + 1);
                rabbitTemplate.send("", cola, mensaje);
                channel.basicAck(deliveryTag, false);
                log.warn("Error transitorio en {}, reintento {}/{}: {}", cola, intento + 1, maxReintentos, e.getMessage());
            } else {
                log.error("Reintentos agotados en {}, se envía a la DLQ: {}", cola, e.getMessage());
                channel.basicNack(deliveryTag, false, false);
            }
        }
    }

    private int intentosPrevios(MessageProperties propiedades) {
        Object valor = propiedades.getHeader(HEADER_REINTENTOS);
        return valor instanceof Number numero ? numero.intValue() : 0;
    }
}
