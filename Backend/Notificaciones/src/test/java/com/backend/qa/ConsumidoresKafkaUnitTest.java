package com.backend.qa;

import com.backend.consumer.inventario.InventarioConsumer;
import com.backend.consumer.notificaciones.BienvenidaConsumer;
import com.backend.consumer.soporte.MensajeInvalidoException;
import com.backend.consumer.tickets.TicketConsumer;
import com.backend.messaging.event.OrdenCreadaEvent;
import com.backend.messaging.event.ProductoEvent;
import com.backend.messaging.event.UsuarioRegistradoEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** QA Kafka: reglas de validacion de los consumidores (sin broker). */
class ConsumidoresKafkaUnitTest {

    private final BienvenidaConsumer bienvenida = new BienvenidaConsumer();
    private final TicketConsumer tickets = new TicketConsumer();
    private final InventarioConsumer inventario = new InventarioConsumer();

    private static OrdenCreadaEvent.Item item(String nombre) {
        return new OrdenCreadaEvent.Item(1L, nombre, 1, 1000.0);
    }

    @Test
    void bienvenida_eventoValido_noLanza() {
        assertThatCode(() -> bienvenida.recibir(
                new UsuarioRegistradoEvent("oid", "Ana", "ana@test.cl", Instant.now()))).doesNotThrowAnyException();
    }

    @Test
    void bienvenida_sinEmail_esMensajeInvalido() {
        assertThatThrownBy(() -> bienvenida.recibir(new UsuarioRegistradoEvent("oid", "Ana", " ", Instant.now())))
                .isInstanceOf(MensajeInvalidoException.class);
        assertThatThrownBy(() -> bienvenida.recibir(new UsuarioRegistradoEvent("oid", "Ana", null, Instant.now())))
                .isInstanceOf(MensajeInvalidoException.class);
    }

    @Test
    void bienvenida_falla_esErrorReintentable() {
        assertThatThrownBy(() -> bienvenida.recibir(
                new UsuarioRegistradoEvent("oid", "Ana #falla", "ana@test.cl", Instant.now())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ticket_ordenValida_noLanza() {
        assertThatCode(() -> tickets.recibir(
                new OrdenCreadaEvent(1L, "oid", List.of(item("Teclado")), 1000.0, Instant.now()))).doesNotThrowAnyException();
    }

    @Test
    void ticket_ordenInvalida_esMensajeInvalido() {
        assertThatThrownBy(() -> tickets.recibir(new OrdenCreadaEvent(null, "oid", List.of(item("x")), 1.0, Instant.now())))
                .isInstanceOf(MensajeInvalidoException.class);
        assertThatThrownBy(() -> tickets.recibir(new OrdenCreadaEvent(1L, "oid", List.of(), 1.0, Instant.now())))
                .isInstanceOf(MensajeInvalidoException.class);
        assertThatThrownBy(() -> tickets.recibir(new OrdenCreadaEvent(1L, "oid", null, 1.0, Instant.now())))
                .isInstanceOf(MensajeInvalidoException.class);
        assertThatThrownBy(() -> tickets.recibir(new OrdenCreadaEvent(1L, "oid", List.of(item("x")), 0.0, Instant.now())))
                .isInstanceOf(MensajeInvalidoException.class);
        assertThatThrownBy(() -> tickets.recibir(new OrdenCreadaEvent(1L, "oid", List.of(item("x")), null, Instant.now())))
                .isInstanceOf(MensajeInvalidoException.class);
    }

    @Test
    void ticket_falla_esErrorReintentable() {
        assertThatThrownBy(() -> tickets.recibir(
                new OrdenCreadaEvent(1L, "oid", List.of(item("Item #falla")), 10.0, Instant.now())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void inventario_eventosValidos_noLanzan() {
        for (String accion : List.of("CREADO", "ACTUALIZADO", "ELIMINADO", "STOCK_BAJO")) {
            assertThatCode(() -> inventario.recibir(new ProductoEvent(7L, "Mouse", 2, accion, Instant.now())))
                    .doesNotThrowAnyException();
        }
        // ELIMINADO viaja con stock null (ver KafkaProductoEventPublisher)
        assertThatCode(() -> inventario.recibir(new ProductoEvent(7L, "Mouse", null, "ELIMINADO", Instant.now())))
                .doesNotThrowAnyException();
    }

    @Test
    void inventario_sinProductoId_esMensajeInvalido() {
        assertThatThrownBy(() -> inventario.recibir(new ProductoEvent(null, "Mouse", 1, "CREADO", Instant.now())))
                .isInstanceOf(MensajeInvalidoException.class);
    }

    @Test
    void inventario_falla_esErrorReintentable() {
        assertThatThrownBy(() -> inventario.recibir(new ProductoEvent(7L, "Mouse #falla", 1, "CREADO", Instant.now())))
                .isInstanceOf(IllegalStateException.class);
    }
}