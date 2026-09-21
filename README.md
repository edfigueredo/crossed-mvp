# CrossEd MVP

CrossEd convierte material de estudio en un crucigrama multijugador. El profesor crea y administra una partida temporal; hasta 60 alumnos ingresan mediante código, enlace o QR y reciben actualizaciones en tiempo real.

## Funcionalidades incluidas

- carga de texto, archivos PDF/Word/PowerPoint/Excel/TXT y enlaces públicos;
- propuestas de conceptos mediante OpenAI, un reintento y respaldo con Gemini;
- conceptos manuales y datos Docker de demostración sin depender de IA;
- edición de palabras y definiciones antes de iniciar;
- generador matricial determinístico y validador independiente;
- registro temporal de hasta 60 alumnos y tokens separados por rol;
- QR, enlace y código de sala;
- WebSocket/STOMP para eventos y REST para acciones;
- servidor autoritativo para tiempo, respuestas, intentos, puntaje y ranking;
- heartbeat cada 5 segundos y eliminación tras más de 15 segundos sin actividad;
- avisos a 3 y 1 minuto, conejito y ambiente arcade progresivo;
- ranking con empates y correo final configurable;
- estado temporal en Redis con TTL de 6 horas;
- interfaz responsive para PC y dispositivos móviles.

## Arquitectura

```text
Navegador Next.js
   | REST + WebSocket/STOMP
   v
Spring Boot 3 · monolito modular
   |-- partida / jugador / crucigrama / ranking
   |-- contenido / ia / correo / tiempo_real / excepcion
   v
Redis · estado temporal
```

El frontend solo dibuja la geometría validada por el backend. El DTO del alumno incluye definición, posición, dirección y longitud; nunca incluye la palabra correcta.

## Inicio rápido con Docker

Requisitos: Docker Engine con Docker Compose v2.

```bash
cp .env.example .env
docker compose up --build
```

Abrir:

- frontend: <http://localhost:3000>
- backend: <http://localhost:8080>
- salud: <http://localhost:8080/actuator/health>

La demostración puede probarse sin claves: **Crear crucigrama → Probar ejemplo Docker**.

Detener:

```bash
docker compose down
```

Detener y borrar el volumen temporal de Redis:

```bash
docker compose down -v
```

## Ejecución tradicional

Requisitos: Java 21, Maven 3.9+, Node.js 22+, npm y Redis 7+.

### 1. Redis

Iniciar Redis en `localhost:6379`.

### 2. Backend

```bash
cd backend
mvn clean verify
mvn spring-boot:run
```

### 3. Frontend de desarrollo

En otra terminal:

```bash
cd frontend
npm ci
npm run dev
```

### Frontend de producción local

Las variables `NEXT_PUBLIC_*` se fijan durante la compilación:

```bash
cd frontend
NEXT_PUBLIC_API_URL=http://localhost:8080 \
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws \
npm run build
node .next/standalone/server.js
```

## Variables de entorno

Copiar `.env.example` a `.env`. No guardar `.env` en el repositorio.

| Variable | Uso | Valor local habitual |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | perfil operativo | `docker` |
| `REDIS_HOST` / `REDIS_PORT` | conexión Redis | `redis` / `6379` en Compose |
| `REDIS_USERNAME` / `REDIS_PASSWORD` | Redis administrado | vacío en local |
| `REDIS_SSL` | TLS de Redis | `false` local; `true` si el proveedor lo exige |
| `OPENAI_API_KEY` | proveedor principal de IA | secreto opcional |
| `GEMINI_API_KEY` | respaldo de IA | secreto opcional |
| `OPENAI_MODEL` | modelo OpenAI | `gpt-4o-mini` |
| `GEMINI_MODEL` | modelo Gemini | `gemini-2.5-flash` |
| `MAIL_HOST` / `MAIL_PORT` | SMTP | proveedor elegido / `587` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | autenticación SMTP | secretos |
| `MAIL_FROM` | remitente | correo verificado |
| `MAIL_AUTH` / `MAIL_STARTTLS` | opciones SMTP | `true` |
| `CORS_ALLOWED_ORIGINS` | orígenes permitidos, separados por coma | `http://localhost:3000` |
| `NEXT_PUBLIC_API_URL` | REST visto por el navegador | `http://localhost:8080` |
| `NEXT_PUBLIC_WS_URL` | WebSocket visto por el navegador | `ws://localhost:8080/ws` |

Si IA o correo no están configurados, la partida manual/de demostración funciona; el sistema conserva el resultado e informa el fallo de correo para permitir reintento.

## Pruebas

Backend:

```bash
cd backend
mvn clean verify
```

Prueba REST real con backend y Redis ya iniciados:

```bash
python scripts/prueba_api.py
```

Prueba WebSocket/STOMP con backend, Redis y dependencias del frontend instaladas:

```bash
node scripts/prueba_websocket.mjs
```

Prueba del recorrido visual con Playwright:

```bash
cd frontend
npx playwright install chromium
npx playwright test
```

El conjunto cubre geometría, conectividad, cruces, ranking, permisos, DTO seguro, desconexión, reingreso, tiempo, avisos, fallo SMTP, 60 conexiones y flujo profesor/alumno.

## API principal

| Método | Ruta | Acceso |
|---|---|---|
| `POST` | `/api/contenido/archivo` | público |
| `POST` | `/api/contenido/enlace` | público |
| `POST` | `/api/crucigramas/conceptos` | público |
| `POST` | `/api/crucigramas/generar` | público |
| `POST` | `/api/partidas` | profesor, creación |
| `GET` | `/api/partidas/codigo/{codigo}` | público |
| `GET` | `/api/partidas/{id}` | token profesor |
| `PUT` | `/api/partidas/{id}/crucigrama` | token profesor |
| `POST` | `/api/partidas/{id}/iniciar` | token profesor |
| `POST` | `/api/partidas/{id}/finalizar` | token profesor |
| `DELETE` | `/api/partidas/{id}` | token profesor |
| `POST` | `/api/partidas/{id}/jugadores` | registro alumno |
| `POST` | `/api/partidas/{id}/jugadores/{jugador}/heartbeat` | token alumno |
| `POST` | `/api/partidas/{id}/jugadores/{jugador}/respuestas` | token alumno |
| `GET` | `/api/partidas/{id}/ranking` | según estado/configuración |

Las operaciones protegidas reciben `X-Token`. WebSocket usa `/ws`, con cabeceras STOMP `X-Token` y `Partida`, y canales `/tema/partida/{id}` o `/tema/profesor/{id}`.

Ejemplos completos: [docs/EJEMPLOS_API.md](docs/EJEMPLOS_API.md).

## Decisiones y límites del MVP

- Las sesiones no son cuentas y expiran en Redis.
- Al perder heartbeat, el alumno y su progreso se eliminan; si vuelve, comienza de cero con el tiempo global vigente.
- Los enlaces de Canva, Gamma, Prezi u otras plataformas funcionan si entregan HTML o texto público. Cuando una plataforma requiere JavaScript o autenticación, debe exportarse el archivo o pegarse el texto.
- YouTube depende de que exista una transcripción pública accesible.
- Archivos escaneados requieren OCR previo.
- El correo usa SMTP; no se incluye un proveedor específico.
- El sistema se diseñó para una instancia de backend en el MVP. Para escalar horizontalmente se necesita coordinación distribuida del broker y de las actualizaciones Redis.
- Las llamadas reales a OpenAI/Gemini y SMTP requieren credenciales del propietario.

## Despliegue futuro

No se realizó ningún despliegue público.

Propuesta portable:

1. frontend Next.js en Vercel o equivalente;
2. backend mediante `backend/Dockerfile` en Render o un servicio Docker con WebSocket;
3. Redis administrado en Upstash o equivalente;
4. configurar HTTPS, `wss://`, secretos, Redis TLS y CORS con el dominio real;
5. comprobar `/actuator/health`, REST, WebSocket y correo antes de abrir acceso.

La URL pública del backend debe asignarse en `NEXT_PUBLIC_API_URL` y `NEXT_PUBLIC_WS_URL` **antes** de construir el frontend.

## Estructura

```text
CrossEd/
├── backend/                 Spring Boot, Maven y pruebas
├── frontend/                Next.js, TypeScript y prueba Playwright
├── infra/redis.conf         Redis local
├── scripts/                 pruebas REST y WebSocket
├── docs/                    arquitectura, ejemplos y requisitos de origen
├── docker-compose.yml
├── .env.example
├── README.md
└── CHECKLIST_ENTREGA.md
```
