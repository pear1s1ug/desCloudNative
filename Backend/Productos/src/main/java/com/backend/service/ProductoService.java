   package com.backend.service;

   import com.backend.dto.ProductoRequest;
   import com.backend.dto.ProductoResponse;
   import com.backend.messaging.producer.ProductoEventPublisher;
   import com.backend.model.CategoriaModel;
   import com.backend.model.ProductoModel;
   import com.backend.repository.CategoriaRepository;
   import com.backend.repository.ProductoRepository;
   import org.springframework.stereotype.Service;
   import org.springframework.transaction.annotation.Transactional;

   import java.util.List;
   import java.util.Optional;

   @Service
   public class ProductoService {

       /** Bajo este stock se publica la alerta de stock bajo. */
       private static final int STOCK_MINIMO = 5;

       private final ProductoRepository productoRepository;
       private final CategoriaRepository categoriaRepository;
       private final ProductoEventPublisher eventPublisher;

       public ProductoService(ProductoRepository productoRepository,
                              CategoriaRepository categoriaRepository,
                              ProductoEventPublisher eventPublisher) {
           this.productoRepository = productoRepository;
           this.categoriaRepository = categoriaRepository;
           this.eventPublisher = eventPublisher;
       }

       public List<ProductoResponse> obtenerTodos() {
           return productoRepository.findAll().stream().map(this::mapToResponse).toList();
       }

       public Optional<ProductoResponse> obtenerPorId(Long id) {
           return productoRepository.findById(id).map(this::mapToResponse);
       }

       @Transactional
       public ProductoResponse crear(ProductoRequest request) {
           ProductoModel nuevo = ProductoModel.builder()
                   .nombre(request.getNombre())
                   .descripcion(request.getDescripcion())
                   .precio(request.getPrecio())
                   .stock(request.getStock())
                   .imagenUrl(request.getImagenUrl())
                   .categoria(buscarCategoria(request.getCategoria()))
                   .build();
           ProductoModel guardado = productoRepository.save(nuevo);

           eventPublisher.productoCreado(guardado.getId(), guardado.getNombre(), guardado.getStock());
           revisarStock(guardado);
           return mapToResponse(guardado);
       }

       @Transactional
       public Optional<ProductoResponse> actualizar(Long id, ProductoRequest request) {
           return productoRepository.findById(id).map(producto -> {
               producto.setNombre(request.getNombre());
               producto.setDescripcion(request.getDescripcion());
               producto.setPrecio(request.getPrecio());
               producto.setStock(request.getStock());
               producto.setImagenUrl(request.getImagenUrl());
               producto.setCategoria(buscarCategoria(request.getCategoria()));
               ProductoModel guardado = productoRepository.save(producto);

               eventPublisher.productoActualizado(guardado.getId(), guardado.getNombre(), guardado.getStock());
               revisarStock(guardado);
               return mapToResponse(guardado);
           });
       }

       @Transactional
       public boolean eliminar(Long id) {
           return productoRepository.findById(id).map(producto -> {
               productoRepository.delete(producto);
               eventPublisher.productoEliminado(producto.getId(), producto.getNombre());
               return true;
           }).orElse(false);
       }

       /** Regla de negocio: si el stock queda bajo el mínimo, se publica la alerta. */
       private void revisarStock(ProductoModel producto) {
           if (producto.getStock() != null && producto.getStock() < STOCK_MINIMO) {
               eventPublisher.stockBajo(producto.getId(), producto.getNombre(), producto.getStock());
           }
       }

       private CategoriaModel buscarCategoria(String nombre) {
           return categoriaRepository.findByNombre(nombre)
                                   .orElseThrow(() -> new IllegalArgumentException("La categoría '" + nombre + "' no existe"));
    }

    private ProductoResponse mapToResponse(ProductoModel p) {
        return ProductoResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .precio(p.getPrecio())
                .stock(p.getStock())
                .imagenUrl(p.getImagenUrl())
                .categoria(p.getCategoria().getNombre())
                .build();
    }
}