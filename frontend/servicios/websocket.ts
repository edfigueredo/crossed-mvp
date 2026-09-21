import { Client } from "@stomp/stompjs";
import type { Sesion } from "../tipos";
export function conectar(s: Sesion, evento: (tipo: string) => void) {
  const cliente = new Client({
    brokerURL: process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:8080/ws",
    connectHeaders: { "X-Token": s.token, Partida: s.idPartida },
    reconnectDelay: 3000,
    heartbeatIncoming: 5000,
    heartbeatOutgoing: 5000,
    onConnect: () => {
      cliente.subscribe(
        `/tema/${s.profesor ? "profesor" : "partida"}/${s.idPartida}`,
        (m) => evento(JSON.parse(m.body).tipo),
      );
    },
  });
  cliente.activate();
  return () => {
    void cliente.deactivate();
  };
}
