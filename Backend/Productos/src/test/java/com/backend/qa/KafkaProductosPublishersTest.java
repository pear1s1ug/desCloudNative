package com.backend.qa;

import com.backend.config.MensajeriaProperties;
import com.backend.messaging.event.OrdenCreadaEvent;
import com.backend.messaging.event.ProductoEvent;
import com.backend.messaging.producer.KafkaOrdenEventPublisher;
import com.backend.messaging.producer.KafkaProductoEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** QA Kafka: los publishers de Productos usan el topic, la key y la accion del contrato (infra/eventos.md). */
@SuppressWarnings("unchecked")
class KafkaProductosPublishersTest {

    static MensajeriaProperties props() {
        return new MensajeriaProperties(
                new MensajeriaProperties.Topics("nexus.usuario.registrado", "nexus.orden.creada", "nexus.producto.eventos"),
                new MensajeriaProperties.Dlt("nexus.usuario.registrado.DLT", "nexus.orden.creada.DLT", "nexus.producto.eventos.DLT"),
                new MensajeriaProperties.ConsumerGroups("nexus.notificaciones", "nexus.tickets", "nexus.inventario"),
                3, (short) 1, 3);
    }

    private final KafkaTemplate<String, ProductoEvent> productoTemplate = mock(KafkaTemplate.class);
    private final KafkaTemplate<String, OrdenCreadaEvent> ordenTemplate = mock(KafkaTemplate.class);
    private final KafkaProductoEventPublisher productos = new KafkaProductoEventPublisher(productoTemplate, props());
    private final KafkaOrdenEventPublisher ordenes = new KafkaOrdenEventPublisher(ordenTemplate, props());

    private ProductoEvent capturarProducto() {
        ArgumentCaptor<ProductoEvent> ev = ArgumentCaptor.forClass(ProductoEvent.class);
        verify(productoTemplate).send(eq("nexus.producto.eventos"), eq("7"), ev.capture());
        return ev.getValue();
    }

    @Test
    void productoCreado_publicaAccionCreado() {
        productos.productoCreado(7L, "Mouse", 10);
        ProductoEvent e = capturarProducto();
        assertThat(e.accion()).isEqualTo("CREADO");
        assertThat(e.stock()).isEqualTo(10);
        assertThat(e.nombre()).isEqualTo("Mouse");
        assertThat(e.fecha()).isNotNull();
    }

    @Test
    void productoActualizado_publicaAccionActualizado() {
        productos.productoActualizado(7L, "Mouse", 8);
        assertThat(capturarProducto().accion()).isEqualTo("ACTUALIZADO");
    }

    @Test
    void productoEliminado_publicaAccionEliminado_conStockNull() {
        productos.productoEliminado(7L, "Mouse");
        ProductoEvent e = capturarProducto();
        assertThat(e.accion()).isEqualTo("ELIMINADO");
        assertThat(e.stock()).isNull();
    }

    @Test
    void stockBajo_publicaAccionStockBajo() {
        productos.stockBajo(7L, "Mouse", 2);
        ProductoEvent e = capturarProducto();
        assertThat(e.accion()).isEqualTo("STOCK_BAJO");
        assertThat(e.stock()).isEqualTo(2);
    }

    @Test
    void ordenCreada_publicaEnTopicOrden_conKeyOrdenId() {
        OrdenCreadaEvent evento = new OrdenCreadaEvent(15L, "oid",
                List.of(new OrdenCreadaEvent.Item(1L, "Teclado", 1, 89990.0)), 89990.0, Instant.now());
        ordenes.ordenCreada(evento);
        verify(ordenTemplate).send("nexus.orden.creada", "15", evento);
    }
}