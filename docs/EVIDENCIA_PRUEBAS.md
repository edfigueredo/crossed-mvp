# Evidencia resumida de pruebas

Verificación realizada el 21/09/2026.

| Control | Resultado |
|---|---|
| Backend `mvn clean verify` con Java 21 | 27 pruebas; 0 fallos; 0 errores |
| Frontend `npm run build` | compilación de producción correcta |
| Frontend `npm run typecheck` | TypeScript correcto |
| `docker compose config --quiet` | configuración válida |
| `/actuator/health` con Redis real | `UP` |
| Flujo REST real | creación, registro, permisos, DTO seguro, respuestas, puntaje y cierre correctos |
| Capacidad | 60 registros y heartbeats concurrentes; número 61 rechazado |
| WebSocket/STOMP | conexión autenticada y evento `PARTIDA_INICIADA` recibido |
| Reconstrucción desde ZIP limpio | Maven y Next.js reconstruidos sin `target`, `.next` ni `node_modules` |
| Fallo SMTP | finalización conservada; estado de correo recuperable |
| Playwright | prueba incluida; ejecución bloqueada por la política de sockets del contenedor de construcción |
| Docker Engine | Compose validado; ejecución bloqueada porque el entorno no permite iniciar el daemon |

## Cobertura unitaria y de servicio

- cruce en la misma celda y coincidencia exacta de letra;
- rechazo de tercera palabra, contacto lateral, extremos ocupados y desconexión geométrica;
- máximo de cruces por longitud y reducción de palabras;
- normalización de tildes conservando `Ñ`;
- ranking, tiempos y empates;
- separación de tokens y DTO del alumno sin soluciones;
- eliminación tras la tolerancia y reingreso con nuevo jugador;
- visibilidad de pantalla sin eliminación;
- alertas de 3 y 1 minuto;
- cierre por tiempo y por finalización de todos;
- límite de 60 alumnos;
- intentos y puntaje calculados por el servidor;
- fallo de correo sin pérdida del resultado.
