package com.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de mensajería de RabbitAdmin.
 * Spring Boot crea automáticamente AmqpAdmin y RabbitTemplate a partir de spring.rabbitmq.
 * Aquí solo se habilita MensajeriaProperties (app.messaging), que define los recursos
 * del sistema que la API no permite borrar ni modificar.
 */
@Configuration
@EnableConfigurationProperties(MensajeriaProperties.class)
public class RabbitConfig {
}