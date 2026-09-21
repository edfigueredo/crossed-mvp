# CrossEd MVP

CrossEd convierte material de estudio en un crucigrama colaborativo para clases. El profesor prepara la actividad, comparte un código o QR, controla la partida en tiempo real y recibe el ranking final. Cada alumno participa desde su navegador y recibe su resultado por correo.

## Funcionalidades incluidas

- Creación y edición de partidas de 5 a 20 conceptos.
- Extracción de texto desde archivos o enlaces.
- Generación de conceptos con OpenAI, Gemini o datos de demostración.
- Crucigrama validado, con respuestas ocultas para alumnos.
- Código visible, enlace y QR de acceso.
- Hasta 60 alumnos, tokens por rol y eliminación por desconexión.
- Inicio, cronómetro, intentos, puntajes, avisos y cierre en tiempo real.
- Ranking privado del profesor y envío de resultados.
- Persistencia de partidas en Redis.
- Correos mediante API HTTPS de Brevo o SMTP como alternativa.

## Arquitectura

| Componente | Tecnología |
| --- | --- |
| Frontend | Next.js 16, React 19, TypeScript |
| Backend | Java 21, Spring Boot 3.5 |
| Tiempo real | WebSocket + STOMP |
| Persistencia | Redis 7 |
| IA | OpenAI y Gemini, con fallback |
| Correo | Brevo API HTTPS o SMTP |
| Contenedores | Docker y Docker Compose |

## Inicio rápido con Docker

Requisitos: Docker Engine o Docker Desktop con Compose.

```bash
cp .env.example .env
docker compose up --build
```

Abrí `http://localhost:3000`. El backend responde en `http://localhost:8080` y su estado se consulta en `http://localhost:8080/actuator/health`.

El ejemplo arranca sin claves externas y permite crear actividades con los conceptos de demostración. Para generación por IA, configurá `GEMINI_API_KEY` u `OPENAI_API_KEY`.

## Ejecución sin Docker

Requisitos: Java 21, Maven 3.9+, Node.js 22 y Redis.

```bash
# Terminal 1
cd backend
mvn spring-boot:run

# Terminal 2
cd frontend
npm ci
npm run dev
```

## Variables principales

Copiá `.env.example` a `.env`; nunca publiques ese archivo.

| Variable | Uso |
| --- | --- |
| `REDIS_HOST`, `REDIS_PORT` | Servidor Redis |
| `REDIS_USERNAME`, `REDIS_PASSWORD`, `REDIS_SSL` | Credenciales y TLS de Redis remoto |
| `GEMINI_API_KEY` | Generación gratuita con Gemini |
| `OPENAI_API_KEY` | Proveedor alternativo de IA |
| `BREVO_API_KEY` | Envío de correo por HTTPS en producción |
| `MAIL_FROM`, `MAIL_FROM_NAME` | Remitente verificado |
| `MAIL_*` | SMTP alternativo para entornos que lo permitan |
| `CORS_ALLOWED_ORIGINS` | URL pública del frontend |
| `NEXT_PUBLIC_API_URL` | URL pública HTTPS del backend |
| `NEXT_PUBLIC_WS_URL` | URL pública WSS del endpoint `/ws` |

## Pruebas

```bash
cd backend
mvn clean verify

cd ../frontend
npm ci
npm run typecheck
npm run build
```

La suite del backend cubre geometría, ranking, permisos, límites, desconexión, tiempo, intentos y cierre. La prueba de navegador está en `frontend/pruebas/flujo.spec.ts`.

## Despliegue gratuito

La arquitectura recomendada está documentada en [`docs/GUIA_DESPLIEGUE_GRATUITO.md`](docs/GUIA_DESPLIEGUE_GRATUITO.md): frontend en Vercel, backend Docker en Render, Redis en Upstash, IA con Gemini y correo con Brevo.

## Documentación

- `docs/ARQUITECTURA.md`: componentes y decisiones técnicas.
- `docs/EJEMPLOS_API.md`: ejemplos mínimos de la API.
- `docs/EVIDENCIA_PRUEBAS.md`: verificaciones realizadas.
- `docs/GUIA_DESPLIEGUE_GRATUITO.md`: procedimiento completo de publicación.
- `CHECKLIST_ENTREGA.md`: control final del entregable.

## Seguridad y límites del MVP

- Los tokens se almacenan con hash y las respuestas no se exponen al alumno.
- No se incluyen secretos en el repositorio.
- No hay cuentas permanentes ni historial entre clases.
- El plan gratuito de Render puede suspender el backend por inactividad; la primera apertura puede demorar aproximadamente un minuto.
- Redis es el estado autoritativo del MVP; el broker STOMP es interno a una única instancia del backend.

