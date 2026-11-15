package com.backend.messaging.producer;

import com.backend.config.MensajeriaProperties;
import com.backend.messaging.event.ProductoEvent;
import com.backend.monitoring.KafkaPublicacionMetricas;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class KafkaProductoEventPublisher implements ProductoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaProductoEventPublisher.class);

    private final KafkaTemplate<String, ProductoEvent> kafkaTemplate;
    private final MensajeriaProperties props;
    private final KafkaPublicacionMetricas metricas;

    @Autowired
    public KafkaProductoEventPublisher(
            KafkaTemplate<String, ProductoEvent> kafkaTemplate,
            MensajeriaProperties props,
            KafkaPublicacionMetricas metricas) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
        this.metricas = metricas;
        metricas.registrarTopic(props.topics().productoEventos());
    }

    /** Sin metricas (tests unitarios). */
    public KafkaProductoEventPublisher(
            KafkaTemplate<String, ProductoEvent> kafkaTemplate,
            MensajeriaProperties props) {
        this(kafkaTemplate, props, new KafkaPublicacionMetricas(new SimpleMeterRegistry()));
    }

    @Override
    public void productoCreado(Long productoId, String nombre, Integer stock) {
        publicar(new ProductoEvent(productoId, nombre, stock, "CREADO", Instant.now()));
    }

    @Override
    public void productoActualizado(Long productoId, String nombre, Integer stock) {
        publicar(new ProductoEvent(productoId, nombre, stock, "ACTUALIZADO", Instant.now()));
    }

    @Override
    public void productoEliminado(Long productoId, String nombre) {
        publicar(new ProductoEvent(productoId, nombre, null, "ELIMINADO", Instant.now()));
    }

    @Override
    public void stockBajo(Long productoId, String nombre, Integer stock) {
        publicar(new ProductoEvent(productoId, nombre, stock, "STOCK_BAJO", Instant.now()));
    }

    private void publicar(ProductoEvent evento) {
        String topic = props.topics().productoEventos();
        String key = String.valueOf(evento.productoId());
        try {
            metricas.monitorear(kafkaTemplate.send(topic, key, evento), topic, key);
        } catch (RuntimeException e) {
            metricas.error(topic);
            throw e;
        }
        log.info("Evento enviado a topic {} con key {}: {}", topic, key, evento);
    }
}