# Checklist de entrega — CrossEd MVP

## Producto

- [x] Flujo completo de profesor y alumno.
- [x] Crucigrama de 5 a 20 conceptos.
- [x] Código, enlace y QR de acceso.
- [x] Máximo de 60 alumnos y correo único por partida.
- [x] Cronómetro, intentos, puntaje, avisos y ranking.
- [x] Cierre manual, automático y por finalización conjunta.
- [x] Resultados por Brevo HTTPS o SMTP.

## Arquitectura y seguridad

- [x] Backend Java 21 / Spring Boot.
- [x] Frontend Next.js / TypeScript.
- [x] Redis y WebSocket/STOMP.
- [x] OpenAI con fallback a Gemini y conceptos de demostración.
- [x] Tokens con hash y autorización separada por rol.
- [x] Respuestas y datos sensibles excluidos de la vista del alumno.
- [x] Validación de entrada, límites de carga y errores controlados.
- [x] `.env.example` sin secretos reales.

## Contenedores y ejecución

- [x] Dockerfile multi-stage para backend.
- [x] Dockerfile multi-stage y usuario no root para frontend.
- [x] `docker-compose.yml` con Redis, backend y frontend.
- [x] Health checks y volumen persistente de Redis.
- [x] Instrucciones locales y de despliegue documentadas.

## Verificaciones

- [x] Suite automatizada de backend.
- [x] Typecheck y build de producción del frontend.
- [x] Integración REST y concurrencia de 60 alumnos.
- [x] Integración WebSocket/STOMP.
- [x] Reconstrucción limpia desde el ZIP.
- [ ] Validación real de Brevo con dominio/remitente del propietario.
- [ ] Smoke test público después de crear los servicios.

Los dos puntos pendientes necesitan las cuentas y credenciales del propietario y se completan durante el despliegue guiado.

