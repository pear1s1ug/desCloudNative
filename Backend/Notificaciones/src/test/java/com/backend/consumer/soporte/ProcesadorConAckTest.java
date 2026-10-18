package com.backend.consumer.soporte;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;

class ProcesadorConAckTest {

    @Test
    void debeEnviarAckCuandoProcesamientoEsExitoso() throws Exception {
        Channel channel = Mockito.mock(Channel.class);
        ProcesadorConAck procesador = new ProcesadorConAck();

        procesador.procesar(() -> {}, 10L, channel);

        verify(channel).basicAck(10L, false);
    }

    @Test
    void debeEnviarNackSinRequeueCuandoLanzaExcepcion() throws Exception {
        Channel channel = Mockito.mock(Channel.class);
        ProcesadorConAck procesador = new ProcesadorConAck();

        procesador.procesar(() -> {
            throw new MensajeInvalidoException("Error invalido");
        }, 10L, channel);

        verify(channel).basicNack(10L, false, false);
    }
}