   package com.backend.messaging.producer;

   import com.backend.config.MensajeriaProperties;
   import com.backend.messaging.event.OrdenCreadaEvent;
   import org.slf4j.Logger;
   import org.slf4j.LoggerFactory;
   import org.springframework.amqp.rabbit.core.RabbitTemplate;
   import org.springframework.stereotype.Component;

   @Component
   public class RabbitOrdenEventPublisher implements OrdenEventPublisher {

       private static final Logger log = LoggerFactory.getLogger(RabbitOrdenEventPublisher.class);

       private final RabbitTemplate rabbitTemplate;
       private final MensajeriaProperties props;

       public RabbitOrdenEventPublisher(RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
           this.rabbitTemplate = rabbitTemplate;
           this.props = props;
       }

       @Override
       public void ordenCreada(OrdenCreadaEvent evento) {
           rabbitTemplate.convertAndSend(props.exchanges().direct(), props.routingKeys().ordenCreada(), evento);
           log.info("Evento publicado en {} con routing key {}: orden {}",
                   props.exchanges().direct(), props.routingKeys().ordenCreada(), evento.ordenId());
       }
   }