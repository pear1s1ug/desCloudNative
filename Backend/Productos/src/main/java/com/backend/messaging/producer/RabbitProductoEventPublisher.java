   package com.backend.messaging.producer;

   import com.backend.config.MensajeriaProperties;
   import com.backend.messaging.event.ProductoEvent;
   import org.slf4j.Logger;
   import org.slf4j.LoggerFactory;
   import org.springframework.amqp.rabbit.core.RabbitTemplate;
   import org.springframework.stereotype.Component;

   import java.time.Instant;

   @Component
   public class RabbitProductoEventPublisher implements ProductoEventPublisher {

       private static final Logger log = LoggerFactory.getLogger(RabbitProductoEventPublisher.class);

       private final RabbitTemplate rabbitTemplate;
       private final MensajeriaProperties props;

       public RabbitProductoEventPublisher(RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
           this.rabbitTemplate = rabbitTemplate;
           this.props = props;
       }

       @Override
       public void productoCreado(Long productoId, String nombre, Integer stock) {
           publicar(props.routingKeys().productoCreado(), new ProductoEvent(productoId, nombre, stock, "CREADO", Instant.now()));
       }

       @Override
       public void productoActualizado(Long productoId, String nombre, Integer stock) {
           publicar(props.routingKeys().productoActualizado(), new ProductoEvent(productoId, nombre, stock, "ACTUALIZADO", Instant.now()));
       }

       @Override
       public void productoEliminado(Long productoId, String nombre) {
           publicar(props.routingKeys().productoEliminado(), new ProductoEvent(productoId, nombre, null, "ELIMINADO", Instant.now()));
       }

       @Override
       public void stockBajo(Long productoId, String nombre, Integer stock) {
           publicar(props.routingKeys().productoStockBajo(), new ProductoEvent(productoId, nombre, stock, "STOCK_BAJO", Instant.now()));
       }

       private void publicar(String routingKey, ProductoEvent evento) {
           rabbitTemplate.convertAndSend(props.exchanges().topic(), routingKey, evento);
           log.info("Evento publicado en {} con routing key {}: {}", props.exchanges().topic(), routingKey, evento);
       }
   }