   package com.backend.config;

   import org.springframework.amqp.core.DirectExchange;
   import org.springframework.amqp.core.ExchangeBuilder;
   import org.springframework.amqp.core.TopicExchange;
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

       // ===== Caso 2: ticket de compra -> nexus.direct, routing key orden.creada =====
       @Bean
       public DirectExchange directExchange() {
           return ExchangeBuilder.directExchange(props.exchanges().direct()).durable(true).build();
       }

       // ===== Caso 3: inventario -> nexus.topic, routing keys producto.* =====
       @Bean
       public TopicExchange topicExchange() {
           return ExchangeBuilder.topicExchange(props.exchanges().topic()).durable(true).build();
       }

       // Mensajes en JSON (los eventos viajan según infra/eventos.md)
       @Bean
       public MessageConverter jsonMessageConverter() {
           return new JacksonJsonMessageConverter();
       }
   }