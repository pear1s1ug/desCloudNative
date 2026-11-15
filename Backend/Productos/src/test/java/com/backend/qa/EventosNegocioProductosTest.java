package com.backend.qa;

import com.backend.dto.OrdenRequest;
import com.backend.dto.OrdenResponse;
import com.backend.dto.ProductoRequest;
import com.backend.messaging.event.OrdenCreadaEvent;
import com.backend.messaging.producer.OrdenEventPublisher;
import com.backend.messaging.producer.ProductoEventPublisher;
import com.backend.model.CategoriaModel;
import com.backend.model.OrdenModel;
import com.backend.model.ProductoModel;
import com.backend.repository.CategoriaRepository;
import com.backend.repository.OrdenRepository;
import com.backend.repository.ProductoRepository;
import com.backend.service.OrdenService;
import com.backend.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** QA Kafka: reglas de negocio que disparan eventos en ProductoService y OrdenService (sin broker ni BD). */
class EventosNegocioProductosTest {

    private final ProductoRepository productoRepo = mock(ProductoRepository.class);
    private final CategoriaRepository categoriaRepo = mock(CategoriaRepository.class);
    private final OrdenRepository ordenRepo = mock(OrdenRepository.class);
    private final ProductoEventPublisher productoPub = mock(ProductoEventPublisher.class);
    private final OrdenEventPublisher ordenPub = mock(OrdenEventPublisher.class);

    private final ProductoService productoService = new ProductoService(productoRepo, categoriaRepo, productoPub);
    private final OrdenService ordenService = new OrdenService(ordenRepo, productoRepo, ordenPub);

    private final CategoriaModel categoria = CategoriaModel.builder().id(1L).nombre("Perifericos").build();

    private ProductoRequest request(int stock) {
        return new ProductoRequest("Mouse", "d", 1000.0, stock, "", "Perifericos");
    }

    private void guardarConId(long id) {
        when(productoRepo.save(any(ProductoModel.class))).thenAnswer(inv -> {
            ProductoModel p = inv.getArgument(0);
            p.setId(id);
            return p;
        });
    }

    @Test
    void crearProducto_conStockNormal_publicaSoloCreado() {
        when(categoriaRepo.findByNombre("Perifericos")).thenReturn(Optional.of(categoria));
        guardarConId(7L);
        productoService.crear(request(10));
        verify(productoPub).productoCreado(7L, "Mouse", 10);
        verify(productoPub, never()).stockBajo(anyLong(), anyString(), anyInt());
    }

    @Test
    void crearProducto_conStockMenorA5_publicaCreadoYStockBajo() {
        when(categoriaRepo.findByNombre("Perifericos")).thenReturn(Optional.of(categoria));
        guardarConId(7L);
        productoService.crear(request(4));
        verify(productoPub).productoCreado(7L, "Mouse", 4);
        verify(productoPub).stockBajo(7L, "Mouse", 4);
    }

    @Test
    void crearProducto_stockIgualA5_noEsStockBajo() {
        when(categoriaRepo.findByNombre("Perifericos")).thenReturn(Optional.of(categoria));
        guardarConId(7L);
        productoService.crear(request(5));
        verify(productoPub, never()).stockBajo(anyLong(), anyString(), anyInt());
    }

    @Test
    void crearProducto_categoriaInexistente_noPublica() {
        when(categoriaRepo.findByNombre(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productoService.crear(request(10))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(productoPub);
    }

    @Test
    void actualizarProducto_publicaActualizado_yStockBajoSiCorresponde() {
        ProductoModel existente = ProductoModel.builder().id(7L).nombre("Mouse").stock(10).precio(1.0).categoria(categoria).build();
        when(productoRepo.findById(7L)).thenReturn(Optional.of(existente));
        when(categoriaRepo.findByNombre("Perifericos")).thenReturn(Optional.of(categoria));
        when(productoRepo.save(any(ProductoModel.class))).thenAnswer(inv -> inv.getArgument(0));
        productoService.actualizar(7L, request(2));
        verify(productoPub).productoActualizado(7L, "Mouse", 2);
        verify(productoPub).stockBajo(7L, "Mouse", 2);
    }

    @Test
    void eliminarProducto_publicaEliminado_yNadaSiNoExiste() {
        ProductoModel existente = ProductoModel.builder().id(7L).nombre("Mouse").stock(10).categoria(categoria).build();
        when(productoRepo.findById(7L)).thenReturn(Optional.of(existente));
        when(productoRepo.findById(99L)).thenReturn(Optional.empty());
        assertThat(productoService.eliminar(7L)).isTrue();
        verify(productoPub).productoEliminado(7L, "Mouse");
        assertThat(productoService.eliminar(99L)).isFalse();
        verify(productoPub, never()).productoEliminado(99L, null);
    }

    @Test
    void crearOrden_publicaOrdenCreada_conItemsYTotal() {
        ProductoModel teclado = ProductoModel.builder().id(1L).nombre("Teclado").stock(10).precio(89990.0).categoria(categoria).build();
        when(ordenRepo.buscarUsuarioIdPorOid("oid-1")).thenReturn(Optional.of(3L));
        when(productoRepo.findById(1L)).thenReturn(Optional.of(teclado));
        when(ordenRepo.save(any(OrdenModel.class))).thenAnswer(inv -> {
            OrdenModel o = inv.getArgument(0);
            o.setId(15L);
            return o;
        });

        OrdenResponse r = ordenService.crear("oid-1", new OrdenRequest(List.of(new OrdenRequest.Item(1L, 2))));

        ArgumentCaptor<OrdenCreadaEvent> ev = ArgumentCaptor.forClass(OrdenCreadaEvent.class);
        verify(ordenPub).ordenCreada(ev.capture());
        OrdenCreadaEvent e = ev.getValue();
        assertThat(e.ordenId()).isEqualTo(15L);
        assertThat(e.usuarioOid()).isEqualTo("oid-1");
        assertThat(e.total()).isEqualTo(179980.0);
        assertThat(e.items()).containsExactly(new OrdenCreadaEvent.Item(1L, "Teclado", 2, 89990.0));
        assertThat(e.fecha()).isNotNull();
        assertThat(r.total()).isEqualTo(e.total());
        assertThat(teclado.getStock()).isEqualTo(8);
    }

    @Test
    void crearOrden_stockInsuficiente_noPublica() {
        ProductoModel teclado = ProductoModel.builder().id(1L).nombre("Teclado").stock(1).precio(10.0).categoria(categoria).build();
        when(ordenRepo.buscarUsuarioIdPorOid("oid-1")).thenReturn(Optional.of(3L));
        when(productoRepo.findById(1L)).thenReturn(Optional.of(teclado));
        assertThatThrownBy(() -> ordenService.crear("oid-1", new OrdenRequest(List.of(new OrdenRequest.Item(1L, 5)))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(ordenPub);
    }

    @Test
    void crearOrden_usuarioSinPerfil_noPublica() {
        when(ordenRepo.buscarUsuarioIdPorOid("desconocido")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ordenService.crear("desconocido", new OrdenRequest(List.of(new OrdenRequest.Item(1L, 1)))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(ordenPub);
    }
}