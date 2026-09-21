package com.crossed.jugador.modelo;

import java.util.*;

public class Jugador {
  public String idJugador, nombre, correo, hashToken;
  public long ultimaActividad, tiempoFinalizacion;
  public boolean finalizo, pantallaActiva = true;
  public Map<Long, ProgresoJugador> progreso = new HashMap<>();

  public int correctas() {
    return (int) progreso.values().stream().filter(p -> p.correcta).count();
  }
}
