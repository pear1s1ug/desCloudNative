package com.backend.consumer.soporte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class DlqMonitor {

    private static final Logger log = LoggerFactory.getLogger(DlqMonitor.class);
    private final RabbitAdmin rabbitAdmin;

    private static final String[] DLQ_QUEUES = {
        "notificaciones.usuario.bienvenida.dlq",
        "notificaciones.ticket.soporte.dlq",
        "notificaciones.inventario.alerta.dlq"
    };

    public DlqMonitor(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    @Scheduled(fixedRate = 10000)
    public void monitorearDlq() {
        for (String queueName : DLQ_QUEUES) {
            Properties props = rabbitAdmin.getQueueProperties(queueName);
            if (props != null) {
                Integer messageCount = (Integer) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
                if (messageCount != null && messageCount > 0) {
                    log.warn("ALERTA DLQ: La cola {} tiene {} mensajes no procesados.", queueName, messageCount);
                }
            }
        }
    }
}