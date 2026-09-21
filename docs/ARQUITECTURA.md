# Arquitectura y decisiones

## Componentes

- **Frontend:** Next.js + React + TypeScript. Renderiza la matriz aprobada, administra la experiencia responsive y escucha eventos STOMP.
- **Backend:** Spring Boot 3 sobre Java 21. Monolito modular con capas dentro de cada módulo.
- **Redis:** estado temporal de partidas, jugadores y progreso; TTL de seis horas.
- **IA:** Spring AI con OpenAI; un reintento breve y respaldo Gemini mediante API compatible.
- **Correo:** SMTP asíncrono mediante tarea programada. El error queda registrado y no revierte el cierre.

## Autoridad del servidor

El navegador envía acciones: registro, heartbeat, visibilidad y respuesta. El servidor decide estado, tiempo, intentos, corrección, puntaje, finalización y ranking.

## Seguridad

- tokens aleatorios de 256 bits almacenados como SHA-256;
- token del profesor distinto de cada token de alumno;
- canales privados del profesor validados durante la suscripción STOMP;
- mensajes enviados por WebSocket rechazados: las acciones usan REST;
- DTO del alumno sin soluciones;
- CORS configurable;
- URLs públicas protegidas contra acceso a redes privadas durante extracción;
- límites para archivos, texto, descargas y mensajes WebSocket;
- errores públicos sin stack trace ni secretos.

## Generación

El generador usa una matriz interna 25×25, prueba hasta 100 órdenes determinísticos, evalúa cruces y compacidad y conserva el mayor conjunto válido. El validador independiente rechaza letras distintas, superposición paralela, contactos laterales, terceras palabras, límites excedidos y componentes desconectados. Luego la matriz se recorta.

## Tiempo real

REST persiste las operaciones. WebSocket/STOMP informa ingresos, desconexiones, visibilidad, inicio, finalización, avisos y jugadores que terminaron. El broker simple es suficiente para una instancia del MVP.
