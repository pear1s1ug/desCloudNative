# Monitoreo de Kafka – Nexus Gaming

Monitoreo de Kafka para el proyecto: **métricas** (Micrometer + Prometheus), **health checks** (Spring Boot Actuator, con un chequeo propio de Kafka) y **alertas** (reglas de Prometheus) para mensajes en los DLT, lag de consumidores, broker caído y servicios caídos o con health DOWN. Incluye un dashboard de Grafana ya configurado.

Implementado y verificado el 15 de noviembre de 2026, entre las 19:00 y las 19:20 (hora de Chile, UTC-3).

---

## 1. Arquitectura

```
                    ┌──────────────────────── Docker (red kafka_kafka_net) ─────────────────────────┐
                    │                                                                               │
 Microservicios     │   kafka (9092 / interno 29092) ◄── kafka-exporter (9308) ◄──┐                 │
 Spring Boot en     │        ▲                                                    │ scrape 15 s     │
 Windows            │        │                                       Prometheus (9090) ──► Grafana (3000)
  ├ Productos 8082 ─┼────────┘  /actuator/prometheus ◄── host.docker.internal ───┘   reglas de alerta  │
  ├ Usuarios 8083   │                                                                               │
  ├ Notificaciones 8084                                                    Kafka UI (8080, sin cambios) │
  └ KafkaAdmin 8085 │                                                                               │
                    └───────────────────────────────────────────────────────────────────────────────┘
```

- **kafka-exporter** (`danielqsj/kafka-exporter:v1.9.0`) lee el lag por grupo de consumidores, los offsets de cada topic (incluidos los `.DLT`) y la cantidad de brokers. Funciona aunque los microservicios estén apagados.
- **Prometheus** (`prom/prometheus:v3.5.0`) recolecta métricas del exporter y de `/actuator/prometheus` de cada servicio, y evalúa las reglas de alerta.
- **Grafana** (`grafana/grafana:12.1.1`) trae el datasource Prometheus y el dashboard **"Nexus Gaming - Kafka"** ya provisionados.
- **Microservicios**: Actuator + `micrometer-registry-prometheus`, un health check `kafka`, métricas de los clientes Kafka (autoconfiguración de Spring Boot) y contadores y gauges propios con prefijo `nexus_`.

No hay Alertmanager: las alertas se ven en Prometheus (`/alerts`) y en Grafana, pero no se envían por correo ni por Slack (ver Pendientes).

## 2. Cómo levantarlo

```powershell
# 1) Broker (si no está arriba)
cd infra\kafka
docker compose up -d

# 2) Monitoreo (usa la red del broker, así que el paso 1 va primero)
cd ..\monitoring
copy .env.example .env   # solo la primera vez; ya existe un .env con una clave generada
docker compose up -d
docker compose ps

# 3) Microservicios (cada uno en su terminal). Prometheus los lee desde Docker vía host.docker.internal
cd Backend\Notificaciones; .\mvnw.cmd spring-boot:run
cd Backend\Usuarios;       .\mvnw.cmd spring-boot:run   # necesita DB_URL, DB_USERNAME, DB_PASSWORD y AZURE_* (en .env)
cd Backend\Productos;      .\mvnw.cmd spring-boot:run   # ídem
cd Backend\RabbitAdmin;    .\mvnw.cmd spring-boot:run   # necesita AZURE_*

# Recargar reglas o config de Prometheus sin reiniciar
Invoke-WebRequest -Method Post http://localhost:9090/-/reload
# Apagar el monitoreo (los datos quedan en los volúmenes monitoring_prometheus_data y monitoring_grafana_data)
cd infra\monitoring; docker compose down
```

## 3. URLs (todas en 127.0.0.1)

| Componente | URL | Notas |
|---|---|---|
| Prometheus | http://localhost:9090 | Alertas: http://localhost:9090/alerts. Targets: http://localhost:9090/targets |
| Grafana | http://localhost:3000 | Usuario y clave en **`infra/monitoring/.env`** (`GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD`, ignorado por git). El dashboard "Nexus Gaming - Kafka" queda como inicio |
| kafka-exporter | http://localhost:9308/metrics | |
| Kafka UI | http://localhost:8080 | Sin cambios |
| Productos | http://localhost:8082/actuator/health · /actuator/prometheus · /actuator/info | Sin JWT (solo GET) |
| Usuarios | http://localhost:8083/actuator/health · /actuator/prometheus · /actuator/info | Sin JWT (solo GET) |
| Notificaciones | http://localhost:8084/actuator/health · /actuator/prometheus · /actuator/info | Antes no tenía servidor HTTP; ahora usa Tomcat en 8084 |
| KafkaAdmin (RabbitAdmin) | http://localhost:8085/actuator/health · /actuator/prometheus · /actuator/info | Sin JWT (solo GET) |

También están disponibles `/actuator/health/liveness` y `/actuator/health/readiness`. Cualquier otro endpoint de Actuator (`env`, `beans`, `metrics`, etc.) **no se expone**: sin token responde 401 y con token responde 404.

## 4. Health checks

- `/actuator/health` muestra el estado de cada componente (`show-components: always`), pero sin detalles internos (`show-details: never`).
- Componente **`kafka`** (`com.backend.monitoring.KafkaHealthIndicator`): llama a `describeCluster()` con un timeout de 3 s. Si el broker no responde, el componente queda DOWN y el servicio responde **503**. Se puede desactivar con `MANAGEMENT_HEALTH_KAFKA_ENABLED=false`.
- Usuarios y Productos además incluyen `db` (PostgreSQL).

## 5. Métricas

### Propias del proyecto (prefijo `nexus_`)

| Métrica | Tipo | Servicio | Descripción |
|---|---|---|---|
| `nexus_kafka_broker_disponible` | gauge | todos | 1 si el servicio se conecta al broker y 0 si no |
| `nexus_servicio_salud` | gauge | todos | 1 si `/actuator/health` está UP y 0 si no |
| `nexus_kafka_eventos_publicados_total{topic,resultado}` | counter | Usuarios, Productos | Eventos publicados, con `resultado="ok"` o `"error"` según la confirmación de Kafka. Los envíos fallidos ahora quedan en el log como ERROR |
| `nexus_kafka_dlt_enviados_total{topic,excepcion}` | counter | Notificaciones | Mensajes que el consumidor mandó a un `.DLT`, con el tipo de excepción |
| `nexus_kafka_dlt_mensajes{topic}` | gauge | Notificaciones | Mensajes guardados en cada `.DLT`. Lo calcula `DlqMonitor` cada 30 s; vale -1 si no pudo revisar |

Todas las métricas llevan la etiqueta `application` (`spring.application.name`).

### Clientes Kafka y Spring Kafka (automáticas)

- `kafka_consumer_*` y `kafka_producer_*`: métricas nativas de los clientes, registradas por la autoconfiguración de Spring Boot. Las de productor aparecen recién después del primer envío, porque el productor se crea en ese momento.
- `spring_kafka_listener_seconds_*` (tiempo y resultado de cada mensaje consumido) y `spring_kafka_template_seconds_*` (envíos).

### kafka-exporter

- `kafka_brokers`
- `kafka_consumergroup_lag{consumergroup,topic,partition}`
- `kafka_topic_partition_current_offset{topic,partition}`
- `kafka_topic_partition_oldest_offset{topic,partition}`

Están filtradas a los topics y grupos que empiezan con `nexus.`.

## 6. Reglas de alerta (`infra/monitoring/prometheus/alertas.yml`)

| Alerta | Expresión (resumen) | Umbral / for | Severidad |
|---|---|---|---|
| `KafkaBrokerCaido` | `up{job="kafka-exporter"} == 0 or kafka_brokers < 1` | 1 min | critical |
| `KafkaConsumerLagAlto` | `sum by (consumergroup, topic) (kafka_consumergroup_lag{topic!~".*\.DLT"}) > 10` | **> 10 mensajes por 2 min** | warning |
| `KafkaMensajesNuevosEnDLT` | `sum by (topic) (increase(kafka_topic_partition_current_offset{topic=~".+\.DLT"}[5m])) > 0` | cualquier mensaje nuevo en 5 min, for 30 s | warning |
| `ServicioCaido` | `up{job="servicios-nexus"} == 0` | 1 min | critical |
| `ServicioNoSaludable` | `nexus_servicio_salud == 0` | 1 min | critical |
| `ServicioSinConexionAKafka` | `nexus_kafka_broker_disponible == 0` | 1 min | critical |
| `PublicacionKafkaConErrores` | `increase(nexus_kafka_eventos_publicados_total{resultado="error"}[5m]) > 0` | inmediato | warning |

Los umbrales están pensados para desarrollo local. Para cambiarlos, edita `alertas.yml` y recarga Prometheus. `KafkaMensajesNuevosEnDLT` se resuelve sola 5 minutos después del último mensaje en el DLT. Los mensajes antiguos se ven en el panel "Mensajes guardados por DLT".

**Importante:** `ServicioCaido` se activa por cada servicio que esté apagado. Si solo levantas algunos servicios, verás esa alerta para los demás.

## 7. Dashboard de Grafana "Nexus Gaming - Kafka"

Tiene 12 paneles:
- Brokers Kafka y estado del kafka-exporter
- Servicios up
- Mensajes guardados en DLT (total y por topic) y mensajes nuevos en DLT
- Lag por grupo / topic (con línea en el umbral 10)
- Mensajes por segundo por topic
- Health por servicio y conexión a Kafka por servicio
- Eventos publicados ok/error
- Envíos a DLT por excepción

## 8. Evidencia de la verificación en vivo (15-11-2026, hora de Chile UTC-3)

1. **Stack arriba:** `docker compose ps` muestra `grafana`, `kafka-exporter` y `prometheus` en estado Up. El exporter reportó `kafka_brokers 1`. Grafana `/api/health` respondió `database: ok, version 12.1.1`. El datasource `Prometheus` y el dashboard `nexus-kafka` (12 paneles) quedaron provisionados, y una consulta a través de Grafana devolvió `kafka_brokers=1`.
2. **Targets de Prometheus:** con los 4 servicios levantados, `kafka-exporter`, `prometheus`, `productos`, `usuarios`, `notificaciones` y `kafkaadmin` estaban todos en **up**. Para Usuarios y Productos usé un PostgreSQL temporal (contenedor `nexus-monitoreo-pg` en 127.0.0.1:55432, con `infra/db/schema.sql`), que borré al terminar.
3. **Health:** los 4 servicios respondieron `200` con `"kafka":{"status":"UP"}` (Usuarios y Productos también con `"db":{"status":"UP"}`). `/actuator/env` respondió 401 en los servicios con seguridad y 404 en Notificaciones (no expuesto).
4. **Alerta de DLT:** a las 19:12:05 envié un `UsuarioRegistradoEvent` con email vacío (JSON válido). `KafkaMensajesNuevosEnDLT{topic="nexus.usuario.registrado.DLT"}` pasó a **pending a las 19:12:20** y a **firing a las 19:12:55**. Notificaciones expuso `nexus_kafka_dlt_enviados_total{excepcion="MensajeInvalidoException",topic="nexus.usuario.registrado.DLT"} 1.0` y `nexus_kafka_dlt_mensajes{topic="nexus.usuario.registrado.DLT"} 2.0`.
5. **Alerta de lag:** detuve Notificaciones a las 19:13:09 y a las 19:13:16 envié 25 eventos válidos. El lag de `nexus.notificaciones` subió a **25**. `KafkaConsumerLagAlto` pasó a **pending a las 19:13:20** y a **firing a las 19:15:26**. `ServicioCaido{servicio="notificaciones"}` pasó a firing a las 19:14:26. Volví a levantar Notificaciones a las 19:15:38: a las 19:16:08 el lag estaba en **0** y las dos alertas se habían resuelto. El log muestra 25 "Correo de bienvenida enviado a Lag …".
6. **Broker caído:** detuve el contenedor `kafka` a las 19:16:28. `/actuator/health` de Productos y Notificaciones respondió **503** en unos 3 s. `ServicioNoSaludable` y `ServicioSinConexionAKafka` (los 4 servicios) pasaron a pending a las 19:16:48 y a firing a las 19:17:48. `KafkaBrokerCaido` pasó a pending a las 19:16:58 y a firing a las 19:17:58. Volví a levantar `kafka` (healthy a las 19:18:14): los 4 health quedaron en UP y no quedó ninguna alerta activa.

No se envió JSON mal formado: el defecto DEF-01 de `QA_KAFKA.md` (bucle infinito con un poison pill) **sigue sin corregirse**. No hizo falta corregirlo para estas pruebas.

## 9. Tests

| Servicio | Resultado | Tests nuevos de monitoreo |
|---|---|---|
| Notificaciones | 21 ejecutados, 0 fallas | `KafkaHealthIndicatorTest` (2: DOWN rápido sin broker, UP con broker embebido) y `MetricasDltEmbeddedTest` (1: el contador DLT sube) |
| Productos | 33 ejecutados, 0 fallas, 1 omitido (test de defecto) | `ActuatorMonitoreoTest` (5) y `KafkaPublicacionMetricasTest` (4) |
| Usuarios | 19 ejecutados, 0 fallas, 1 omitido (test de defecto) | `ActuatorMonitoreoTest` (5) y `KafkaPublicacionMetricasTest` (4) |
| RabbitAdmin | 20 ejecutados, 0 fallas, 1 omitido (test de defecto) | `ActuatorMonitoreoTest` (5) |

Los 44 tests del QA anterior y los tests originales siguen pasando.

## 10. Pendiente y limitaciones

- **Contadores de publicación en vivo:** `nexus_kafka_eventos_publicados_total` aparece en 0 en Usuarios y Productos, pero no se probó con eventos reales. Publicar requiere llamar `/api/usuarios/me` o `/api/productos` / `/api/ordenes` con un JWT real de Azure AD, que no está disponible localmente. Los contadores están cubiertos por tests unitarios.
- **Sin notificaciones:** no hay Alertmanager, así que las alertas no envían correo, Slack ni Teams. Si se necesita, hay que agregar Alertmanager al compose y configurarlo en `prometheus.yml`.
- **Endpoint Prometheus público:** `/actuator/prometheus` queda público en el puerto de cada servicio (útil en local). En la nube no debe publicarse en el API Gateway; conviene usar un puerto de management separado (`management.server.port`) o una red privada.
- **Health incluye Kafka:** si Kafka cae, el health del servicio queda DOWN (503). En Kubernetes conviene dejar Kafka solo en el grupo `readiness`.
- **Defectos del QA sin corregir:** DEF-01 (poison pill) sigue abierto. DEF-05 se corrigió en parte: los envíos fallidos ahora se registran y se cuentan, pero la publicación sigue ocurriendo dentro de `@Transactional`.
- **Tests que publican en el broker real (DEF-04):** cada `mvnw test` de Productos y Usuarios sigue publicando un evento en `localhost:9092`, lo que suma un poco de lag hasta que Notificaciones lo consume.