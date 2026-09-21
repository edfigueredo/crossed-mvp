package com.crossed.tiempo_real.servicio;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ServicioEventos {
  @Autowired private SimpMessagingTemplate mensajeria;

  public void emitir(String partida, String tipo) {
    mensajeria.convertAndSend("/tema/partida/" + partida, Map.of("tipo", tipo));
    mensajeria.convertAndSend("/tema/profesor/" + partida, Map.of("tipo", tipo));
  }
}
