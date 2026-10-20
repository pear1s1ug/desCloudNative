package com.backend.consumer.soporte;

import com.backend.config.MensajeriaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Alerta de mensajes fallidos: cada 30 s revisa las DLQ y registra un WARN si tienen mensajes.
 * Las DLQ no se consumen automáticamente, para que los mensajes queden disponibles para análisis.
 */
@Component
public class DlqMonitor {

    private static final Logger log = LoggerFactory.getLogger(DlqMonitor.class);

    private final AmqpAdmin amqpAdmin;
    private final List<String> dlqs;

    public DlqMonitor(AmqpAdmin amqpAdmin, MensajeriaProperties props) {
        this.amqpAdmin = amqpAdmin;
        this.dlqs = List.of(props.dlq().notificaciones(), props.dlq().tickets(), props.dlq().inventario());
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void revisarDlqs() {
        for (String dlq : dlqs) {
            try {
                QueueInformation info = amqpAdmin.getQueueInfo(dlq);
                if (info != null && info.getMessageCount() > 0) {
                    log.warn("[ALERTA DLQ] {} tiene {} mensaje(s) sin procesar", dlq, info.getMessageCount());
                }
            } catch (AmqpException e) {
                log.warn("No se pudo revisar {}: {}", dlq, e.getMessage());
            }
        }
    }
}
