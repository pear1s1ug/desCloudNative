package com.backend.messaging.producer;

import com.backend.config.MensajeriaProperties;
import com.backend.messaging.event.UsuarioRegistradoEvent;
import com.backend.monitoring.KafkaPublicacionMetricas;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class KafkaUsuarioEventPublisher implements UsuarioEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaUsuarioEventPublisher.class);

    private final KafkaTemplate<String, UsuarioRegistradoEvent> kafkaTemplate;
    private final MensajeriaProperties props;
    private final KafkaPublicacionMetricas metricas;

    @Autowired
    public KafkaUsuarioEventPublisher(
            KafkaTemplate<String, UsuarioRegistradoEvent> kafkaTemplate,
            MensajeriaProperties props,
            KafkaPublicacionMetricas metricas) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
        this.metricas = metricas;
        metricas.registrarTopic(props.topics().usuarioRegistrado());
    }

    /** Sin metricas (tests unitarios). */
    public KafkaUsuarioEventPublisher(
            KafkaTemplate<String, UsuarioRegistradoEvent> kafkaTemplate,
            MensajeriaProperties props) {
        this(kafkaTemplate, props, new KafkaPublicacionMetricas(new SimpleMeterRegistry()));
    }

    @Override
    public void usuarioRegistrado(String azureOid, String nombre, String email) {
        UsuarioRegistradoEvent evento = new UsuarioRegistradoEvent(azureOid, nombre, email, Instant.now());
        String topic = props.topics().usuarioRegistrado();
        try {
            metricas.monitorear(kafkaTemplate.send(topic, azureOid, evento), topic, azureOid);
        } catch (RuntimeException e) {
            metricas.error(topic);
            throw e;
        }
        log.info("Evento enviado a topic {} con key {}: {}", topic, azureOid, evento);
    }
}