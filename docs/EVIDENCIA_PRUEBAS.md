# Evidencia de pruebas

## Verificaciones ejecutadas antes de la entrega

- `mvn clean verify`: suite del backend aprobada.
- `npm run typecheck`: tipos del frontend aprobados.
- `npm run build`: build de producción de Next.js aprobado.
- Flujo REST: creación, ingreso, inicio, respuestas, cierre y ranking.
- Concurrencia: registro del máximo de 60 alumnos y rechazo del número 61.
- WebSocket/STOMP: conexión autenticada, suscripción autorizada y evento recibido.
- Reconstrucción limpia: backend y frontend reconstruidos desde el contenido del ZIP.

## Restricciones del entorno de validación

El daemon Docker y un navegador gráfico no estaban disponibles en el entorno de creación. Por eso se verificaron individualmente Dockerfiles, builds, health checks y servicios de integración. El smoke test final con proveedores reales se completa durante el despliegue guiado.

