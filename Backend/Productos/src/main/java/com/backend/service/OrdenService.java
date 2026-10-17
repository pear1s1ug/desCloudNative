   package com.backend.service;

   import com.backend.dto.OrdenRequest;
   import com.backend.dto.OrdenResponse;
   import com.backend.messaging.event.OrdenCreadaEvent;
   import com.backend.messaging.producer.OrdenEventPublisher;
   import com.backend.model.DetalleOrdenModel;
   import com.backend.model.OrdenModel;
   import com.backend.model.ProductoModel;
   import com.backend.repository.OrdenRepository;
   import com.backend.repository.ProductoRepository;
   import org.springframework.stereotype.Service;
   import org.springframework.transaction.annotation.Transactional;

   import java.time.Instant;
   import java.time.LocalDateTime;
   import java.util.ArrayList;
   import java.util.List;

   @Service
   public class OrdenService {

       private final OrdenRepository ordenRepository;
       private final ProductoRepository productoRepository;
       private final OrdenEventPublisher eventPublisher;

       public OrdenService(OrdenRepository ordenRepository,
                           ProductoRepository productoRepository,
                           OrdenEventPublisher eventPublisher) {
           this.ordenRepository = ordenRepository;
           this.productoRepository = productoRepository;
           this.eventPublisher = eventPublisher;
       }

       @Transactional
       public OrdenResponse crear(String usuarioOid, OrdenRequest request) {
           Long usuarioId = ordenRepository.buscarUsuarioIdPorOid(usuarioOid)
                   .orElseThrow(() -> new IllegalArgumentException("El usuario no tiene un perfil registrado"));

           OrdenModel orden = OrdenModel.builder()
                   .usuarioId(usuarioId)
                   .fecha(LocalDateTime.now())
                   .total(0.0)
                   .build();

           List<OrdenCreadaEvent.Item> items = new ArrayList<>();
           double total = 0;

           for (OrdenRequest.Item item : request.getItems()) {
               ProductoModel producto = productoRepository.findById(item.getProductoId())
                       .orElseThrow(() -> new IllegalArgumentException("El producto " + item.getProductoId() + " no existe"));

               if (producto.getStock() < item.getCantidad()) {
                   throw new IllegalArgumentException("Stock insuficiente para '" + producto.getNombre() + "'");
               }
               producto.setStock(producto.getStock() - item.getCantidad());

               orden.getDetalles().add(DetalleOrdenModel.builder()
                       .productoId(producto.getId())
                       .cantidad(item.getCantidad())
                       .precioUnitario(producto.getPrecio())
                       .build());

               items.add(new OrdenCreadaEvent.Item(
                       producto.getId(), producto.getNombre(), item.getCantidad(), producto.getPrecio()));
               total += producto.getPrecio() * item.getCantidad();
           }

           orden.setTotal(total);
           OrdenModel guardada = ordenRepository.save(orden);

           eventPublisher.ordenCreada(new OrdenCreadaEvent(
                   guardada.getId(), usuarioOid, items, total, Instant.now()));

           return new OrdenResponse(guardada.getId(), total, items.size());
       }
   }