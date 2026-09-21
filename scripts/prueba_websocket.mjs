#!/usr/bin/env node
import { Client } from "../frontend/node_modules/@stomp/stompjs/esm6/index.js";

const API = process.env.BACKEND_URL || "http://localhost:8080";
const WS = process.env.WS_URL || "ws://localhost:8080/ws";

const conceptos = [
  "CONTENEDOR",
  "IMAGEN",
  "DOCKER",
  "VOLUMEN",
  "PUERTO",
  "RED",
  "SERVICIO",
  "REGISTRO",
  "CAPA",
  "COMPOSE",
].map((palabra) => ({ palabra, definicion: `Definición de ${palabra}` }));

async function api(ruta, metodo = "GET", datos, token) {
  const respuesta = await fetch(API + ruta, {
    method: metodo,
    headers: {
      ...(datos === undefined ? {} : { "Content-Type": "application/json" }),
      ...(token ? { "X-Token": token } : {}),
    },
    body: datos === undefined ? undefined : JSON.stringify(datos),
  });
  if (!respuesta.ok) throw new Error(`${metodo} ${ruta}: ${respuesta.status}`);
  const texto = await respuesta.text();
  return texto ? JSON.parse(texto) : undefined;
}

const partida = await api("/api/partidas", "POST", {
  nombre: "Prueba WebSocket",
  materia: "Docker",
  nombreProfesor: "Docente",
  correoProfesor: "docente@example.test",
  idioma: "es",
  duracionMinutos: 5,
  mostrarResultados: true,
  intentosPorPalabra: 0,
  cantidadPalabrasSolicitada: 5,
  conceptos,
});
const ruta = `/api/partidas/${partida.idPartida}`;

try {
  const jugador = await api(`${ruta}/jugadores`, "POST", {
    nombre: "Alumno WebSocket",
    correo: "ws@example.test",
  });

  await new Promise((resolver, rechazar) => {
    const temporizador = setTimeout(() => {
      void cliente.deactivate();
      rechazar(new Error("No llegó PARTIDA_INICIADA por WebSocket"));
    }, 10000);
    const cliente = new Client({
      brokerURL: WS,
      connectHeaders: {
        "X-Token": jugador.token,
        Partida: partida.idPartida,
      },
      reconnectDelay: 0,
      onStompError: (marco) => rechazar(new Error(marco.headers.message)),
      onWebSocketError: () => rechazar(new Error("Falló la conexión WebSocket")),
      onConnect: () => {
        cliente.subscribe(`/tema/partida/${partida.idPartida}`, async (mensaje) => {
          const evento = JSON.parse(mensaje.body);
          if (evento.tipo === "PARTIDA_INICIADA") {
            clearTimeout(temporizador);
            await cliente.deactivate();
            resolver();
          }
        });
        void api(`${ruta}/iniciar`, "POST", undefined, partida.token).catch(rechazar);
      },
    });
    cliente.activate();
  });
  console.log("OK: conexión STOMP autenticada y evento PARTIDA_INICIADA recibido.");
} finally {
  await api(ruta, "DELETE", undefined, partida.token);
}
