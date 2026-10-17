# Contrato de eventos · Nexus Gaming

Formato de los mensajes que viajan por RabbitMQ (JSON). Productores y consumidores
definen cada uno su propio `record`, con **exactamente estos nombres y tipos de campo**.

Las fechas (`Instant`) viajan como texto ISO-8601 en UTC, por ejemplo `2026-10-17T20:15:00Z`.

---

## 1. UsuarioRegistradoEvent

| | |
|---|---|
| Productor | Usuarios (al crear un perfil nuevo en `sincronizar()`) |
| Exchange | `nexus.direct` (direct) |
| Routing key | `usuario.registrado` |
| Cola | `nexus.notificaciones.queue` |

```java
public record UsuarioRegistradoEvent(
        String azureOid,
        String nombre,
        String email,
        Instant fecha) {}
```

```json
{
  "azureOid": "0cec6a06-200b-487b-b74d-ffd9982ed70a",
  "nombre": "Admin Test",
  "email": "admin.test@tntcarladescloud.onmicrosoft.com",
  "fecha": "2026-10-17T20:15:00Z"
}
```

---

## 2. OrdenCreadaEvent

| | |
|---|---|
| Productor | Productos (`POST /api/ordenes`) |
| Exchange | `nexus.direct` (direct) |
| Routing key | `orden.creada` |
| Cola | `nexus.tickets.queue` |

```java
public record OrdenCreadaEvent(
        Long ordenId,
        String usuarioOid,
        List<Item> items,
        Double total,
        Instant fecha) {

    public record Item(
            Long productoId,
            String nombre,
            Integer cantidad,
            Double precioUnitario) {}
}
```

```json
{
  "ordenId": 15,
  "usuarioOid": "8bf58edc-96b8-42d5-91f0-b1c5030c48a8",
  "items": [
    { "productoId": 1, "nombre": "Teclado 60% Custom Wireless Hotswap", "cantidad": 1, "precioUnitario": 89990.0 }
  ],
  "total": 89990.0,
  "fecha": "2026-10-17T20:15:00Z"
}
```

---

## 3. ProductoEvent

| | |
|---|---|
| Productor | Productos (crear, editar, borrar; stock bajo 5) |
| Exchange | `nexus.topic` (topic) |
| Routing key | `producto.creado` · `producto.actualizado` · `producto.eliminado` · `producto.stock.bajo` |
| Cola | `nexus.inventario.queue` (binding `producto.#`) |

```java
public record ProductoEvent(
        Long productoId,
        String nombre,
        Integer stock,
        String accion,   // CREADO | ACTUALIZADO | ELIMINADO | STOCK_BAJO
        Instant fecha) {}
```

```json
{
  "productoId": 7,
  "nombre": "Mouse de prueba",
  "stock": 2,
  "accion": "STOCK_BAJO",
  "fecha": "2026-10-17T20:15:00Z"
}
```

---

## Probar desde la consola de RabbitMQ

*Exchanges* → elegir el exchange → *Publish message*:

- **Routing key:** la del evento.
- **Properties:** `content_type` = `application/json`.
- **Payload:** el JSON de ejemplo.