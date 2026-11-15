# QA de Kafka – Nexus Gaming (desCloudNative)

**Fecha de ejecución:** 9 de noviembre de 2026, 23:17–23:40 (hora de Chile, UTC-3)
**Veredicto general:** ⚠️ **Kafka funciona de forma PARCIAL.** Los flujos principales (publicar, consumir, reintentar y enviar a DLT) funcionan contra el broker real, pero hay **1 defecto de severidad Alta** (un mensaje JSON mal formado bloquea al consumidor en un bucle infinito) y varios defectos de severidad Media o Baja.

| Resumen | Cantidad |
|---|---|
| Casos de prueba | 26 |
| OK | 22 |
| FALLA | 3 |
| BLOQUEADO | 1 |
| Tests automáticos agregados (activos) | 44 (todos pasan) |
| Tests de defectos (apagados por defecto, fallan a propósito) | 3 |

---

## 1. Alcance

- **Infraestructura:** `infra/kafka/compose.yaml` (Kafka 3.9.1 en modo KRaft + Kafka UI), `infra/messaging-config.yml`, `infra/eventos.md`.
- **Productores:** `Productos` (`KafkaProductoEventPublisher`, `KafkaOrdenEventPublisher`, y las reglas de `ProductoService` y `OrdenService`) y `Usuarios` (`KafkaUsuarioEventPublisher` y `UsuarioService.sincronizar`).
- **Consumidores:** `Notificaciones` (`BienvenidaConsumer`, `TicketConsumer`, `InventarioConsumer`, `DlqMonitor` y `KafkaConfig` con `DefaultErrorHandler` y `DeadLetterPublishingRecoverer`).
- **Administración:** `RabbitAdmin` (en realidad es el servicio KafkaAdmin: `KafkaAdminService` y `KafkaAdminController`).
- **Fuera de alcance:** `Login` (BFF, no usa Kafka), el Frontend y API Gateway/AWS (`evidencias.sh` apunta a la nube; no se ejecutó).

## 2. Entorno

| Elemento | Valor |
|---|---|
| SO | Windows (equipo Notebook) |
| Java | 22.0.2 (los pom piden 17) |
| Spring Boot / Spring Kafka / kafka-clients | 4.0.8 / 4.0.7 / 4.1.2 |
| Docker | Docker Desktop 4.99.0, Engine 29.9.3 |
| Broker | Contenedor `kafka` (apache/kafka:3.9.1) y `kafka-ui` (v0.7.2), **ya estaban corriendo** desde las 23:13 (levantados con `infra/kafka/compose.yaml`). Estado: healthy |
| Base de datos | No disponible: no hay `.env` con `DB_URL`, y el contenedor `pg-local` está detenido |
| Azure AD (JWT) | No disponible para pruebas locales |

## 3. Revisión estática (consistencia)

| Ítem | Resultado |
|---|---|
| Nombres de los topics (`nexus.usuario.registrado`, `nexus.orden.creada`, `nexus.producto.eventos`) y de los `.DLT` | Coinciden en `messaging-config.yml`, en los 4 `application.yml`, en los `application.properties` de test y en `eventos.md` ✅ |
| Grupos de consumidores (`nexus.notificaciones`, `nexus.tickets`, `nexus.inventario`) | Coinciden ✅ |
| Records de eventos (copias en el productor y en el consumidor) | Tienen los mismos campos, tipos y paquete (`com.backend.messaging.event`) ✅ |
| Serialización | El productor usa `StringSerializer` + `JsonSerializer` (sin type headers). El consumidor usa `StringDeserializer` + `JsonDeserializer` con `trusted.packages` y `value.default.type` por listener. Son coherentes ✅ |
| Keys | `azureOid`, `ordenId` y `productoId` como String, tal como pide el contrato ✅ |
| Errores en el consumidor | `DefaultErrorHandler` con `FixedBackOff(1000 ms, 3)`. `MensajeInvalidoException` no se reintenta. El DLT se publica en `<topic>.DLT` en la misma partición ✅. **No hay `ErrorHandlingDeserializer`** ❌ (DEF-01) |
| acks e idempotencia | No están configurados. Se usan los valores por defecto de kafka-clients 4.x (`acks=all`, `enable.idempotence=true`) ✅ |
| Confirmación del envío | `kafkaTemplate.send()` se usa sin manejar el resultado y dentro de `@Transactional` ❌ (DEF-05) |
| Seguridad del broker | PLAINTEXT sin autenticación, expuesto solo en 127.0.0.1. Kafka UI no tiene login. Sirve para desarrollo local, pero no para la nube (observación) |
| ¿Los tests necesitan broker? | Notificaciones no lo necesita (`listener.auto-startup=false`). **Productos y Usuarios sí intentan publicar en `localhost:9092`** ❌ (DEF-04) |

## 4. Casos de prueba

| ID | Caso | Pasos | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|
| TC-01 | Broker disponible | `docker ps`; `kafka-topics.sh --list` | Broker healthy | `kafka Up (healthy)`. Topics listados | OK |
| TC-02 | Topics del sistema | `kafka-topics.sh --describe` | 6 topics con 3 particiones y RF 1 | 3 topics y 3 DLT, todos con `PartitionCount: 3`, `ReplicationFactor: 1` | OK |
| TC-03 | Consistencia de nombres y config | Revisión estática (sección 3) | Iguales en todos los archivos | Iguales | OK |
| TC-04 | Contrato de campos productor-consumidor | Revisión estática + `ContratoEventos*JsonTest` | Mismos campos que `eventos.md` | Coinciden, y el round-trip con JsonSerializer/JsonDeserializer da OK | OK |
| TC-05 | Suites de test existentes | `mvnw.cmd test` en los 4 servicios | Todas pasan | Notificaciones 1/1, RabbitAdmin 7/7, Productos 7/7, Usuarios 5/5 | OK |
| TC-06 | Productor real → broker → consumidor | Los tests de Productos/Usuarios publican con un `KafkaTemplate` real en el broker Docker. Luego se levanta Notificaciones | El consumidor procesa el evento | `Correo de bienvenida enviado a Ana Pérez (ana@test.cl)` y `Auditoría de inventario: producto Mouse (id 1) CREADO, stock 5` | OK |
| TC-07 | Bienvenida: evento válido | console-producer → `nexus.usuario.registrado` | Log de correo enviado | `Correo de bienvenida enviado a QA Valido (qa1@test.cl)` | OK |
| TC-08 | Compatibilidad con campos extra | Evento con el campo `campoNuevo` | Se ignora el campo y se procesa | `Correo de bienvenida enviado a QA Extra` | OK |
| TC-09 | Usuario sin email | `email:""` | DLT sin reintentos | Llega a `nexus.usuario.registrado.DLT` con el header `cause-fqcn=MensajeInvalidoException` | OK |
| TC-10 | Ticket: orden válida | Orden 9001 | Log del ticket | `Ticket generado para la orden #9001: 1 producto(s), total $2000.0` | OK |
| TC-11 | Orden sin ítems | Orden 9002 con `items:[]` | DLT sin reintentos | Llega a `nexus.orden.creada.DLT` (MensajeInvalidoException) | OK |
| TC-12 | Falla transitoria con reintentos | Orden 9003 con un ítem `#falla` | 3 reintentos cada ~1 s y luego DLT | `Record in retry` a las 23:22:42, 23:22:43 y 23:22:44, y luego llega al DLT con `IllegalStateException` | OK |
| TC-13 | Alerta de stock bajo | ProductoEvent `STOCK_BAJO` | WARN de alerta | `[ALERTA STOCK BAJO] Mouse QA (id 7) quedó con 2 unidad(es)` | OK |
| TC-14 | Producto sin id | `productoId:null` | DLT | Llega a `nexus.producto.eventos.DLT` | OK |
| TC-15 | Monitor de DLT | Esperar 30 s | WARN por cada DLT con mensajes | `[ALERTA DLT] nexus.orden.creada.DLT tiene 2 mensaje(s)…` (y lo mismo para los otros 2 DLT) | OK |
| TC-16 | Fecha numérica (formato real de los productores) | Consumir eventos con `fecha: 1794277248.458…` | Se deserializa bien | Se procesan (TC-06 y test embebido) | OK |
| TC-17 | **JSON mal formado (poison pill)** | `pp-1\|{esto no es json` → `nexus.producto.eventos` y después un evento válido | El mensaje va a DLT (o se salta) y el siguiente se procesa | **Bucle infinito** de `RecordDeserializationException` desde 23:24:00 hasta que se detuvo el proceso a las 23:26:41. Log de **6,8 GB en ~2 min 41 s**. El evento válido "Despues del veneno" (otra partición, mismo listener) **no se procesó** (lag 1) | **FALLA** |
| TC-18 | Formato de fecha según el contrato | `ContratoEventos*JsonTest -Dqa.defectos=true` | `"fecha":"2026-10-17T20:15:00Z"` | Se emite como número: `1.7922681E9` | **FALLA** |
| TC-19 | Admin: no permitir borrar topics internos `__*` | `KafkaAdminServiceEmbeddedTest -Dqa.defectos=true` | 409 sin consultar a Kafka | Pasa la validación y llega a Kafka (`RecursoNoEncontradoException` porque en el broker embebido `__consumer_offsets` todavía no existía). No hay protección | **FALLA** |
| TC-20 | Admin: crear, consultar y borrar un topic | Test embebido | 201/200/204 | OK (`Topic eliminado: qa.admin.topic`) | OK |
| TC-21 | Admin: topics del sistema protegidos y prefijo `__` al crear | Test embebido | ConflictoException (409) | OK | OK |
| TC-22 | Admin: topic duplicado, inexistente o con RF mayor que los brokers | Test embebido | 409 / 404 / KafkaException (502) | OK | OK |
| TC-23 | Reglas de negocio que publican (Productos) | `EventosNegocioProductosTest` | CREADO, ACTUALIZADO, ELIMINADO y STOCK_BAJO (<5). Orden con ítems y total. No se publica si hay error | 9/9 OK | OK |
| TC-24 | Publicación solo de usuarios nuevos (Usuarios) | `KafkaUsuariosTest` | Nuevo → publica. Existente → no publica | OK | OK |
| TC-25 | Flujo completo de consumidores en un broker embebido | `KafkaConsumidoresEmbeddedTest` | Se procesan los válidos; DLT con headers; 4 entregas con `#falla` | 8/8 OK | OK |
| TC-26 | E2E con los servicios Usuarios y Productos levantados (REST) | Levantar con BD y llamar `/api/usuarios/me` y `/api/ordenes` | Se publica el evento | No se pudo levantar: falta PostgreSQL (`DB_URL` sin `.env`, `pg-local` detenido) y no hay un JWT real de Azure AD. Está cubierto en parte por TC-06 y TC-23/24 | BLOQUEADO |

## 5. Evidencias (resumen de los comandos)

Los logs completos están en `qa-kafka-evidencias/`:

- `baseline-*.log`: suites originales, `BUILD SUCCESS` en los 4 servicios.
- `qa-Notificaciones.log`: `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
- `qa-Productos.log`: `Tests run: 24, Failures: 0, Errors: 0, Skipped: 1`, `BUILD SUCCESS`.
- `qa-Usuarios.log`: `Tests run: 10, Failures: 0, Errors: 0, Skipped: 1`, `BUILD SUCCESS`.
- `qa-RabbitAdmin.log`: `Tests run: 15, Failures: 0, Errors: 0, Skipped: 1`, `BUILD SUCCESS`.
- `qa-*-defectos.log`: ejecuciones con `-Dqa.defectos=true`, con `BUILD FAILURE` esperado (confirma DEF-02 y DEF-03).
- `e2e-notificaciones-extracto.log` y `e2e-notificaciones-RESUMEN.txt`: Notificaciones contra el broker Docker. **El log original pesaba 6.800.686.683 bytes (~6,8 GB)** por el bucle del poison pill. Se reemplazó por un extracto (arranque, casos E2E y las primeras ~300 líneas del bucle) y un resumen con conteos: en las primeras 3.000.000 de líneas había 477.094 líneas con `RecordDeserializationException`/`JsonParseException`. El log original se borró para no dejar ~6,8 GB en el disco.

Fragmento del error de TC-17:
```
ERROR ... o.a.k.c.c.internals.CompletedFetch : [Consumer clientId=consumer-nexus.inventario-3, groupId=nexus.inventario] Value Deserializers with error ...
Caused by: org.apache.kafka.common.errors.RecordDeserializationException: Error deserializing VALUE for partition nexus.producto.eventos-2 at offset 0. If needed, please seek past the record to continue consumption.
Caused by: com.fasterxml.jackson.core.JsonParseException: Unexpected character ('e' (code 101)): was expecting double-quote to start field name
```

Headers del DLT (TC-09):
```
kafka_dlt-exception-cause-fqcn: com.backend.consumer.soporte.MensajeInvalidoException
kafka_dlt-original-topic: nexus.usuario.registrado
kafka_dlt-original-consumer-group: nexus.notificaciones
```

Formato real de `fecha` en el topic (TC-18):
```
oid-123 {"azureOid":"oid-123","nombre":"Ana Pérez","email":"ana@test.cl","fecha":1794277248.458217200}
```

## 6. Defectos

| ID | Severidad | Descripción | Evidencia | Corrección propuesta |
|---|---|---|---|---|
| DEF-01 | **Alta** | Notificaciones usa `JsonDeserializer` directo, sin `ErrorHandlingDeserializer`. Un mensaje que no es JSON válido provoca un bucle infinito de poll: el offset nunca avanza, el log crece unos GB por minuto y el listener (las 3 particiones) deja de procesar mensajes válidos. El DLT nunca recibe ese mensaje. | TC-17 | En `Notificaciones/application.yml`: `value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer` y en `properties`: `spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer`. `DefaultErrorHandler` trata `DeserializationException` como no reintentable y la manda al DLT. Para publicar los bytes originales en el DLT, conviene usar un `KafkaTemplate` con `ByteArraySerializer` (o `DelegatingByTypeSerializer`) en el `DeadLetterPublishingRecoverer`. |
| DEF-02 | Media | El contrato (`eventos.md`) dice que `fecha` viaja como texto ISO-8601, pero Productos y Usuarios la emiten como epoch decimal (`1794277248.458217200`). Los consumidores Java lo aceptan, pero rompe el contrato y la interoperabilidad, y en Kafka UI se ve como número. | TC-18, `defecto_fechaDebeViajarComoTextoIso8601` | Configurar el `JsonSerializer` con un ObjectMapper que tenga `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` deshabilitado. Por ejemplo, un `DefaultKafkaProducerFactoryCustomizer` que haga `setValueSerializer(new JsonSerializer<>(mapper).noTypeInfo())`. Hacer lo mismo en el template de DLT de Notificaciones. |
| DEF-03 | Media | `KafkaAdminService.eliminarTopic` solo bloquea los topics del sistema, no el prefijo reservado `__`. Además, la regex `Validaciones.NOMBRE` acepta `__` en la ruta. `DELETE /api/admin/kafka/topics/__consumer_offsets` llega hasta Kafka. | TC-19 | En `eliminarTopic`, rechazar `nombre.startsWith("__")` con `ConflictoException` (reutilizar `validarNombreLibre`). No se probó contra el broker Docker qué hace Kafka al intentar borrar `__consumer_offsets`, para no arriesgar el entorno. |
| DEF-04 | Media | Los tests `ProductoSecurityTest.admin_crea_201` y `UsuarioSecurityTest.me_creaPerfilDesdeClaims_200` publican eventos reales en `localhost:9092`, es decir, ensucian el broker de desarrollo. Cada `mvnw test` dejó un evento "Mouse CREADO" y uno "oid-123" que después consumió Notificaciones. No se verificó cómo se comportan sin broker (probablemente quedan bloqueados en `max.block.ms`). | Offsets antes y después de los tests, y TC-06 | En esos tests, usar `@MockitoBean ProductoEventPublisher/UsuarioEventPublisher`, o `@EmbeddedKafka` (agregar `spring-kafka-test` con scope test). |
| DEF-05 | Media | Los publishers hacen `kafkaTemplate.send()` sin revisar el `CompletableFuture`. Siempre registran "Evento publicado…", aunque el envío falle. Además, publican dentro de `@Transactional` antes del commit: si la BD hace rollback queda un evento "fantasma", y si Kafka falla el evento se pierde sin aviso. | Revisión estática | `send(...).whenComplete((r, ex) -> { if (ex != null) log.error(...); })`. Publicar después del commit (`@TransactionalEventListener(phase = AFTER_COMMIT)`) o usar el patrón Outbox. |
| DEF-06 | Baja | `OrdenService` descuenta stock, pero no publica `STOCK_BAJO` (ni `ACTUALIZADO`) cuando una compra deja el stock en menos de 5. `eventos.md` indica que Productos emite "stock bajo 5". | Revisión estática | Inyectar `ProductoEventPublisher` en `OrdenService` y llamar a `stockBajo(...)` si `stock < 5` después de descontar. |
| DEF-07 | Baja | Los reintentos son bloqueantes: mientras un mensaje se reintenta (~3 s), los demás mensajes del mismo listener esperan. La orden 9001 se procesó recién cuando la 9003 llegó al DLT. | TC-12 (23:22:44.79) | Es aceptable para este volumen. Si se necesita, usar `@RetryableTopic` (reintentos no bloqueantes). |
| DEF-08 | Baja | El DLT guarda el mensaje re-serializado, no el original. La `fecha` ISO enviada aparece como epoch en el DLT. | Contenido del DLT de 9002 y 9003 | Se corrige con DEF-01 (template de bytes para el DLT) y DEF-02. |

## 7. Tests agregados (solo archivos nuevos; no se modificó código de producción ni ningún `pom.xml`)

| Servicio | Archivo | Tests |
|---|---|---|
| Notificaciones | `src/test/java/com/backend/qa/ConsumidoresKafkaUnitTest.java` | 9 unitarios: validaciones de los 3 consumidores y qué excepción es reintentable y cuál no |
| Notificaciones | `src/test/java/com/backend/qa/KafkaConsumidoresEmbeddedTest.java` | 8 con `@EmbeddedKafka`: procesamiento real, DLT con headers, 4 entregas (1 + 3 reintentos), campos extra, fecha numérica y misma partición en el DLT |
| Productos | `src/test/java/com/backend/qa/KafkaProductosPublishersTest.java` | 5: topic, key y acción de cada publisher |
| Productos | `src/test/java/com/backend/qa/EventosNegocioProductosTest.java` | 9: reglas que publican eventos (stock < 5, eliminar, orden, errores que no publican) |
| Productos | `src/test/java/com/backend/qa/ContratoEventosProductosJsonTest.java` | 2 de contrato JSON + 1 de defecto (DEF-02) |
| Usuarios | `src/test/java/com/backend/qa/KafkaUsuariosTest.java` | 4 (publisher, nuevo vs. existente, contrato JSON) + 1 de defecto (DEF-02) |
| RabbitAdmin (KafkaAdmin) | `src/test/java/com/backend/qa/KafkaAdminServiceEmbeddedTest.java` | 7 con `@EmbeddedKafka` + 1 de defecto (DEF-03) |

Los tests nuevos **no usan el broker de Docker**: usan mocks o un broker embebido. Los tests de defectos están anotados con `@EnabledIfSystemProperty(named="qa.defectos", matches="true")` para que el build normal quede en verde. Cuando se corrija cada defecto, se puede quitar esa anotación.

No hay un test automático del poison pill (DEF-01): reproducirlo genera GB de log en pocos minutos. Queda documentado con la evidencia E2E. Después de aplicar la corrección, conviene agregar un test embebido que envíe JSON inválido y verifique que llega a `nexus.producto.eventos.DLT`.

## 8. Cómo volver a ejecutar

```powershell
# Suites completas (no necesitan Docker; los tests de Productos/Usuarios intentan publicar en localhost:9092, ver DEF-04)
cd Backend\Notificaciones; .\mvnw.cmd test
cd ..\Productos;          .\mvnw.cmd test
cd ..\Usuarios;           .\mvnw.cmd test
cd ..\RabbitAdmin;        .\mvnw.cmd test

# Ver los defectos (estos tests deben FALLAR hasta que se corrijan)
.\mvnw.cmd test -Dqa.defectos=true

# Prueba manual E2E con el broker Docker
cd infra\kafka; docker compose up -d
cd ..\..\Backend\Notificaciones; .\mvnw.cmd spring-boot:run
# en otra terminal:
'k1|{"azureOid":"k1","nombre":"Ana","email":"a@test.cl","fecha":"2026-10-17T20:15:00Z"}' |
  docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 `
  --topic nexus.usuario.registrado --property parse.key=true --property "key.separator=|"
```
⚠️ **No envíes JSON inválido hasta corregir DEF-01**: llena el disco con logs.

## 9. Estado en que quedó el entorno

- Los contenedores `kafka` y `kafka-ui` **siguen corriendo**, porque ya estaban levantados antes del QA. No se detuvieron.
- Notificaciones (`spring-boot:run`) se levantó para el E2E y **se detuvo**. No quedan procesos Java del QA.
- En los topics quedaron los mensajes de prueba del QA (keys `qa-oid-*`, `9001`–`9003`, `7`, `x`, `8`, `pp-1`) y 4 mensajes en los DLT. También quedaron los eventos que publican los tests de Productos y Usuarios (DEF-04).
- Para que Notificaciones no vuelva a entrar en bucle al arrancar, se movió el offset del grupo `nexus.inventario` en `nexus.producto.eventos` partición 2 al offset 1. Así se salta el mensaje inválido `pp-1` que se inyectó en la prueba. Quedan pendientes 2 mensajes válidos en `nexus.inventario` (partición 0) y 1 en `nexus.notificaciones` (partición 2); se procesarán la próxima vez que se levante el servicio.