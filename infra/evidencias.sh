#!/usr/bin/env bash
# Evidencias API Gateway - Nexus Gaming (Evaluación 2)
# Uso: API="https://xxxx.execute-api.us-east-1.amazonaws.com" TOKEN_ADMIN="..." TOKEN_CLIENTE="..." bash infra/evidencias.sh

API="${API:?Falta API (Invoke URL del API Gateway)}"
ADMIN="${TOKEN_ADMIN:?Falta TOKEN_ADMIN}"
CLIENTE="${TOKEN_CLIENTE:?Falta TOKEN_CLIENTE}"

PROD='{"nombre":"Mouse de prueba curl","descripcion":"Creado en evidencias","precio":19990,"stock":3,"imagenUrl":"","categoria":"Periféricos"}'
ORDEN='{"items":[{"productoId":1,"cantidad":1}]}'
OK=0; FAIL=0

# prueba "descripcion" METODO RUTA TOKEN CODIGO_ESPERADO [BODY]
prueba() {
  local desc="$1" method="$2" path="$3" token="$4" expected="$5" body="$6"
  local args=(-s -o /tmp/resp.txt -w "%{http_code}" -X "$method" "$API$path")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$body" ] && args+=(-H "Content-Type: application/json" -d "$body")
  local code; code=$(curl "${args[@]}")
  if [ "$code" = "$expected" ]; then
    echo -e "\e[32m✔ $code\e[0m $method $path — $desc"; OK=$((OK+1))
  else
    echo -e "\e[31m✘ $code (esperado $expected)\e[0m $method $path — $desc"; FAIL=$((FAIL+1))
  fi
  echo "   $(head -c 300 /tmp/resp.txt)"
}

echo -e "\n===== 1. SIN TOKEN (401 del API Gateway) ====="
for r in "GET /api/productos" "GET /api/productos/1" "POST /api/productos" "PUT /api/productos/1" "DELETE /api/productos/1" \
         "GET /api/usuarios" "GET /api/usuarios/me" "GET /api/usuarios/1" "PUT /api/usuarios/1" "DELETE /api/usuarios/1" \
         "GET /api/me" "POST /api/ordenes" "POST /api/admin/rabbit/queues"; do
  set -- $r; prueba "sin token" "$1" "$2" "" 401
done

echo -e "\n===== 2. TOKEN INVÁLIDO (401 del API Gateway) ====="
prueba "token falso" GET /api/productos "token-invalido-123" 401
prueba "token falso" GET /api/me "token-invalido-123" 401

echo -e "\n===== 3. ADMIN (acceso completo) ====="
prueba "listar productos" GET /api/productos "$ADMIN" 200
prueba "ver producto 1" GET /api/productos/1 "$ADMIN" 200
prueba "crear producto" POST /api/productos "$ADMIN" 201 "$PROD"
PID=$(grep -o '"id":[0-9]*' /tmp/resp.txt | head -1 | cut -d: -f2)
prueba "editar producto creado" PUT "/api/productos/$PID" "$ADMIN" 200 "${PROD/curl/curl editado}"
prueba "borrar producto creado" DELETE "/api/productos/$PID" "$ADMIN" 204
prueba "listar usuarios" GET /api/usuarios "$ADMIN" 200
prueba "mi perfil" GET /api/usuarios/me "$ADMIN" 200
UID_ADMIN=$(grep -o '"id":[0-9]*' /tmp/resp.txt | head -1 | cut -d: -f2)
prueba "ver usuario" GET "/api/usuarios/$UID_ADMIN" "$ADMIN" 200
prueba "editar usuario" PUT "/api/usuarios/$UID_ADMIN" "$ADMIN" 200 '{"nombre":"Admin Test"}'
prueba "borrar usuario inexistente" DELETE /api/usuarios/9999 "$ADMIN" 404
prueba "claims del token (BFF)" GET /api/me "$ADMIN" 200

echo -e "\n===== 4. CLIENTE (lectura y compra sí, administración no) ====="
prueba "listar productos" GET /api/productos "$CLIENTE" 200
prueba "ver producto 1" GET /api/productos/1 "$CLIENTE" 200
prueba "mi perfil" GET /api/usuarios/me "$CLIENTE" 200
prueba "claims del token (BFF)" GET /api/me "$CLIENTE" 200
prueba "crear orden (compra)" POST /api/ordenes "$CLIENTE" 201 "$ORDEN"
prueba "orden sin productos" POST /api/ordenes "$CLIENTE" 400 '{"items":[]}'
prueba "crear producto" POST /api/productos "$CLIENTE" 403 "$PROD"
prueba "editar producto" PUT /api/productos/1 "$CLIENTE" 403 "$PROD"
prueba "borrar producto" DELETE /api/productos/1 "$CLIENTE" 403
prueba "listar usuarios" GET /api/usuarios "$CLIENTE" 403
prueba "ver usuario" GET /api/usuarios/1 "$CLIENTE" 403
prueba "editar usuario" PUT /api/usuarios/1 "$CLIENTE" 403 '{"nombre":"Hack"}'
prueba "borrar usuario" DELETE /api/usuarios/1 "$CLIENTE" 403
prueba "crear cola (admin RabbitMQ)" POST /api/admin/rabbit/queues "$CLIENTE" 403 '{"nombre":"evidencias.queue"}'

echo -e "\n===== 5. ADMINISTRADOR DE RABBITMQ (rol Admin) ====="
prueba "cola con nombre vacío" POST /api/admin/rabbit/queues "$ADMIN" 400 '{"nombre":""}'
prueba "nombre reservado amq.*" POST /api/admin/rabbit/queues "$ADMIN" 409 '{"nombre":"amq.prueba"}'
prueba "tipo de exchange inválido" POST /api/admin/rabbit/exchanges "$ADMIN" 400 '{"nombre":"evidencias.exchange","tipo":"OTRO"}'
prueba "crear cola con DLQ" POST /api/admin/rabbit/queues "$ADMIN" 201 '{"nombre":"evidencias.queue","conDlq":true,"ttlMs":60000}'
prueba "consultar cola" GET /api/admin/rabbit/queues/evidencias.queue "$ADMIN" 200
prueba "crear exchange topic" POST /api/admin/rabbit/exchanges "$ADMIN" 201 '{"nombre":"evidencias.exchange","tipo":"TOPIC"}'
prueba "crear binding" POST /api/admin/rabbit/bindings "$ADMIN" 201 '{"cola":"evidencias.queue","exchange":"evidencias.exchange","routingKey":"evidencia.#"}'
prueba "borrar cola del sistema" DELETE /api/admin/rabbit/queues/nexus.tickets.queue "$ADMIN" 409
prueba "borrar exchange creado" DELETE /api/admin/rabbit/exchanges/evidencias.exchange "$ADMIN" 204
prueba "borrar cola creada" DELETE /api/admin/rabbit/queues/evidencias.queue "$ADMIN" 204
prueba "borrar su DLQ" DELETE /api/admin/rabbit/queues/evidencias.queue.dlq "$ADMIN" 204
prueba "borrar cola inexistente" DELETE /api/admin/rabbit/queues/no-existe "$ADMIN" 404

echo -e "\n===== RESULTADO: $OK correctas, $FAIL fallidas ====="