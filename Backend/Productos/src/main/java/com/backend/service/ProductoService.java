package com.backend.service;

import com.backend.dto.ProductoRequest;
import com.backend.dto.ProductoResponse;
import com.backend.model.CategoriaModel;
import com.backend.model.ProductoModel;
import com.backend.repository.CategoriaRepository;
import com.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ==========================================
 * CAPA DE SERVICIO (LÓGICA DE NEGOCIO)
 * ==========================================
 */
@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(ProductoRepository productoRepository, CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
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
        return mapToResponse(productoRepository.save(nuevo));
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
            return mapToResponse(productoRepository.save(producto));
        });
    }

    @Transactional
    public boolean eliminar(Long id) {
        if (productoRepository.existsById(id)) {
            productoRepository.deleteById(id);
            return true;
        }
        return false;
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