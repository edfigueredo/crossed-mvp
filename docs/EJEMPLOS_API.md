# Ejemplos de API

Definir primero:

```bash
API=http://localhost:8080
```

## Salud

```bash
curl "$API/actuator/health"
```

## Generar un tablero sin IA

```bash
curl -X POST "$API/api/crucigramas/generar" \
  -H 'Content-Type: application/json' \
  -d '{
    "cantidad": 5,
    "conceptos": [
      {"palabra":"CONTENEDOR","definicion":"Unidad aislada que ejecuta una aplicación."},
      {"palabra":"IMAGEN","definicion":"Plantilla inmutable para crear contenedores."},
      {"palabra":"DOCKER","definicion":"Plataforma para construir y ejecutar contenedores."},
      {"palabra":"VOLUMEN","definicion":"Conserva datos fuera de la capa escribible."},
      {"palabra":"PUERTO","definicion":"Punto de comunicación de un servicio."},
      {"palabra":"RED","definicion":"Comunica contenedores."},
      {"palabra":"SERVICIO","definicion":"Componente definido en Compose."},
      {"palabra":"REGISTRO","definicion":"Repositorio de imágenes."}
    ]
  }'
```

## Extraer un archivo

```bash
curl -X POST "$API/api/contenido/archivo" \
  -F 'archivo=@material.pdf'
```

## Leer un enlace público

```bash
curl -X POST "$API/api/contenido/enlace" \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://ejemplo.org/material-publico"}'
```

## Autorización

Al crear una partida, la respuesta contiene `idPartida`, `codigoVisible` y el token temporal del profesor. Guardar el token solo en la sesión del navegador.

```bash
curl "$API/api/partidas/ID_PARTIDA" \
  -H 'X-Token: TOKEN_PROFESOR'
```

El registro del alumno devuelve su propio `idJugador` y token. La respuesta enviada por el alumno contiene únicamente la acción; el backend calcula el resultado:

```bash
curl -X POST "$API/api/partidas/ID_PARTIDA/jugadores/ID_JUGADOR/respuestas" \
  -H 'Content-Type: application/json' \
  -H 'X-Token: TOKEN_ALUMNO' \
  -d '{"idPalabra":1,"respuesta":"contenedor"}'
```
