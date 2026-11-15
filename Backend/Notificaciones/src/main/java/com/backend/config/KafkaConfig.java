package com.backend.config;

import com.backend.consumer.soporte.MensajeInvalidoException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@EnableConfigurationProperties(MensajeriaProperties.class)
public class KafkaConfig {

    private final MensajeriaProperties props;

    public KafkaConfig(MensajeriaProperties props) {
        this.props = props;
    }

    @Bean
    public NewTopic usuarioRegistradoTopic() {
        return topic(props.topics().usuarioRegistrado());
    }

    @Bean
    public NewTopic ordenCreadaTopic() {
        return topic(props.topics().ordenCreada());
    }

    @Bean
    public NewTopic productoEventosTopic() {
        return topic(props.topics().productoEventos());
    }

    @Bean
    public NewTopic usuarioRegistradoDlt() {
        return topic(props.dlt().usuarioRegistrado());
    }

    @Bean
    public NewTopic ordenCreadaDlt() {
        return topic(props.dlt().ordenCreada());
    }

    @Bean
    public NewTopic productoEventosDlt() {
        return topic(props.dlt().productoEventos());
    }

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<Object, Object> template) {
        return new DeadLetterPublishingRecoverer(template, (record, ex) ->
                new TopicPartition(record.topic() + ".DLT", record.partition()));
    }

    /**
     * Ademas de publicar en el DLT, cuenta cada mensaje enviado:
     * nexus_kafka_dlt_enviados_total{topic="<topic>.DLT", excepcion="..."}.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer, MeterRegistry registry) {
        ConsumerRecordRecoverer recovererConMetricas = (record, ex) -> {
            recoverer.accept(record, ex);
            Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
            Counter.builder("nexus.kafka.dlt.enviados")
                    .description("Mensajes enviados a un topic DLT por el consumidor")
                    .tag("topic", record.topic() + ".DLT")
                    .tag("excepcion", causa.getClass().getSimpleName())
                    .register(registry)
                    .increment();
        };
        DefaultErrorHandler handler = new DefaultErrorHandler(
                recovererConMetricas,
                new FixedBackOff(1000L, props.maxReintentos()));
        handler.addNotRetryableExceptions(MensajeInvalidoException.class);
        return handler;
    }

    private NewTopic topic(String name) {
        return new NewTopic(name, props.partitions(), props.replicationFactor());
    }
}
