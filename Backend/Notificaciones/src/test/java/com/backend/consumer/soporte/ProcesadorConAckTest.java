package com.backend.consumer.soporte;

import com.backend.config.MensajeriaProperties;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Verifica cada fila de la tabla de decisiones de ProcesadorConAck. */
class ProcesadorConAckTest {

    private static final long TAG = 10L;
    private static final String COLA = "cola.test";

    private Channel channel;
    private RabbitTemplate rabbitTemplate;
    private ProcesadorConAck procesador;

    @BeforeEach
    void setUp() {
        channel = mock(Channel.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        MensajeriaProperties props = new MensajeriaProperties(null, null, null, null, 0, 0, 3);
        procesador = new ProcesadorConAck(rabbitTemplate, props);
    }

    private Message mensaje(Integer reintentosPrevios) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setDeliveryTag(TAG);
        propiedades.setConsumerQueue(COLA);
        if (reintentosPrevios != null) {
            propiedades.setHeader(ProcesadorConAck.HEADER_REINTENTOS, reintentosPrevios);
        }
        return new Message("{}".getBytes(), propiedades);
    }

    @Test
    void exito_haceAck() throws Exception {
        procesador.procesar(mensaje(null), channel, () -> {});

        verify(channel).basicAck(TAG, false);
    }

    @Test
    void datoInvalido_vaDirectoALaDlq() throws Exception {
        procesador.procesar(mensaje(null), channel, () -> {
            throw new MensajeInvalidoException("dato inválido");
        });

        verify(channel).basicNack(TAG, false, false);
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    void errorTransitorio_reintentaEnLaMismaCola() throws Exception {
        Message m = mensaje(null);
        procesador.procesar(m, channel, () -> {
            throw new IllegalStateException("falla transitoria");
        });

        verify(rabbitTemplate).send("", COLA, m);
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void reintentosAgotados_vaALaDlq() throws Exception {
        procesador.procesar(mensaje(3), channel, () -> {
            throw new IllegalStateException("falla transitoria");
        });

        verify(channel).basicNack(TAG, false, false);
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }
}
