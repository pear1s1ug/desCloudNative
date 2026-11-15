package com.backend.messaging.producer;

import com.backend.config.MensajeriaProperties;
import com.backend.messaging.event.OrdenCreadaEvent;
import com.backend.monitoring.KafkaPublicacionMetricas;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaOrdenEventPublisher implements OrdenEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrdenEventPublisher.class);

    private final KafkaTemplate<String, OrdenCreadaEvent> kafkaTemplate;
    private final MensajeriaProperties props;
    private final KafkaPublicacionMetricas metricas;

    @Autowired
    public KafkaOrdenEventPublisher(
            KafkaTemplate<String, OrdenCreadaEvent> kafkaTemplate,
            MensajeriaProperties props,
            KafkaPublicacionMetricas metricas) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
        this.metricas = metricas;
        metricas.registrarTopic(props.topics().ordenCreada());
    }

    /** Sin metricas (tests unitarios). */
    public KafkaOrdenEventPublisher(
            KafkaTemplate<String, OrdenCreadaEvent> kafkaTemplate,
            MensajeriaProperties props) {
        this(kafkaTemplate, props, new KafkaPublicacionMetricas(new SimpleMeterRegistry()));
    }

    @Override
    public void ordenCreada(OrdenCreadaEvent evento) {
        String topic = props.topics().ordenCreada();
        String key = String.valueOf(evento.ordenId());
        try {
            metricas.monitorear(kafkaTemplate.send(topic, key, evento), topic, key);
        } catch (RuntimeException e) {
            metricas.error(topic);
            throw e;
        }
        log.info("Evento enviado a topic {} con key {}: orden {}", topic, key, evento.ordenId());
    }
}