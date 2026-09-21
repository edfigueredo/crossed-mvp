# Ejemplos de API

Base local: `http://localhost:8080`.

## Estado

```bash
curl http://localhost:8080/actuator/health
```

## Crear una partida

```bash
curl -X POST http://localhost:8080/api/partidas \
  -H 'Content-Type: application/json' \
  -d '{
    "nombre":"Clase de Docker",
    "materia":"Tecnología",
    "nombreProfesor":"Ada",
    "correoProfesor":"ada@example.com",
    "idioma":"es",
    "duracionMinutos":20,
    "mostrarResultados":true,
    "intentosPorPalabra":2,
    "cantidadPalabrasSolicitada":5,
    "conceptos":[
      {"palabra":"DOCKER","definicion":"Plataforma de contenedores."},
      {"palabra":"IMAGEN","definicion":"Plantilla inmutable."},
      {"palabra":"PUERTO","definicion":"Punto de comunicación."},
      {"palabra":"VOLUMEN","definicion":"Almacenamiento persistente."},
      {"palabra":"RED","definicion":"Comunicación entre servicios."}
    ]
  }'
```

La respuesta incluye `idPartida`, `codigoVisible` y el token del profesor. En las rutas protegidas enviá ese valor en `X-Token`.

## Operaciones principales

| Método | Ruta | Rol |
| --- | --- | --- |
| `POST` | `/api/partidas` | Público |
| `GET` | `/api/partidas/codigo/{codigo}` | Público |
| `GET` | `/api/partidas/{id}` | Profesor |
| `POST` | `/api/partidas/{id}/iniciar` | Profesor |
| `POST` | `/api/partidas/{id}/finalizar` | Profesor |
| `GET` | `/api/partidas/{id}/ranking` | Profesor |
| `POST` | `/api/partidas/{id}/jugadores` | Público |
| `POST` | `/api/partidas/{id}/jugadores/{jid}/respuestas` | Alumno |

WebSocket/STOMP se conecta a `/ws`, enviando `X-Token` y `Partida` en el `CONNECT`. Los alumnos se suscriben a `/tema/partida/{id}` y el profesor a `/tema/profesor/{id}`.

