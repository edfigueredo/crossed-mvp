# Checklist de entrega — CrossEd MVP

Fecha de verificación: 21/09/2026.

## Implementado

- [x] Backend Java 21 + Spring Boot 3 + Maven.
- [x] Frontend Next.js + React + TypeScript responsive.
- [x] Monolito modular con módulos del SDD.
- [x] Redis temporal, TTL e índice de partidas.
- [x] Carga de texto, archivos admitidos y enlaces públicos.
- [x] OpenAI con reintento y Gemini como respaldo secuencial.
- [x] Conceptos manuales y demostración Docker sin IA.
- [x] Generador matricial y validador independiente.
- [x] Edición y regeneración previa del crucigrama.
- [x] QR, enlace y código visible.
- [x] Registro temporal y límite de 60 alumnos.
- [x] REST y WebSocket/STOMP autenticado.
- [x] Heartbeat, visibilidad, desconexión y reingreso desde cero.
- [x] Tiempo, alertas de 3/1 minutos y cierre automático/manual/completo.
- [x] Puntaje, ranking y empates.
- [x] Correo SMTP, conservación del resultado y reintento.
- [x] Conejito, ambiente, mensajes y créditos rotativos.
- [x] Tokens separados y respuestas correctas fuera del DTO del alumno.
- [x] Dockerfiles multi-stage, Compose, health checks y volumen Redis.
- [x] `.env.example`, README, ejemplos y documentación de despliegue futuro.

## Pruebas ejecutadas

- [x] `mvn clean verify`: 27 pruebas, 0 fallos, 0 errores.
- [x] `npm run build`: compilación de producción y TypeScript correctos.
- [x] `docker compose config --quiet`: configuración válida.
- [x] Backend + Redis reales: `/actuator/health` respondió `UP`.
- [x] Flujo REST real: creación, autorización, DTO seguro, respuesta, puntaje, cierre y fallo SMTP.
- [x] Capacidad: 60 registros y 60 heartbeats concurrentes; participante 61 rechazado.
- [x] WebSocket/STOMP real: conexión autenticada y recepción de `PARTIDA_INICIADA`.
- [x] Reconstrucción desde el ZIP limpio: `npm ci && npm run build` y `mvn clean verify` correctos.
- [ ] Recorrido Playwright: preparado, pero el navegador no pudo ejecutarse en el entorno de construcción porque la política del contenedor impide crear el socket interno de Chrome.
- [ ] `docker compose up --build`: no ejecutado aquí porque el entorno de construcción impide iniciar Docker Engine. Los Dockerfiles y Compose quedaron validados estáticamente.
- [ ] Proveedores externos: llamadas reales de IA y SMTP no ejecutadas porque no se suministraron credenciales.

## Cómo levantar

```bash
cp .env.example .env
docker compose up --build
```

Abrir <http://localhost:3000>. Para detener: `docker compose down`.

## Variables que debe completar el propietario

Para IA: `OPENAI_API_KEY` y, si desea respaldo, `GEMINI_API_KEY`.

Para correo: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` y `MAIL_FROM`.

En producción: `REDIS_*`, `CORS_ALLOWED_ORIGINS`, `NEXT_PUBLIC_API_URL` y `NEXT_PUBLIC_WS_URL` usando HTTPS/WSS.

## Pendientes antes de producción

- ejecutar `docker compose up --build` en una computadora con Docker;
- probar al menos una generación real con cada proveedor configurado;
- enviar un correo real y comprobar remitente/SPAM;
- ejecutar `npx playwright test` en un sistema donde Chromium pueda crear sockets;
- realizar una prueba de aula con varios dispositivos;
- autorizar y realizar el despliegue público por separado.
