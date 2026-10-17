package com.backend.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MensajeriaProperties.class)
public class RabbitConfig {

    private final MensajeriaProperties props;

    public RabbitConfig(MensajeriaProperties props) {
        this.props = props;
    }

    // ===== Caso 1: bienvenida -> nexus.direct, routing key usuario.registrado =====
    @Bean
    public DirectExchange directExchange() {
        return ExchangeBuilder.directExchange(props.exchanges().direct()).durable(true).build();
    }

    // Mensajes en JSON (los eventos viajan según infra/eventos.md)
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}