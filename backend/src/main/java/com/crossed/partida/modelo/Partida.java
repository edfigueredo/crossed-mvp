package com.crossed.partida.modelo;

import com.crossed.crucigrama.modelo.Crucigrama;
import com.crossed.jugador.modelo.Jugador;
import com.crossed.partida.dto.CrearPartidaDto;
import java.util.*;

public class Partida {
  public String idPartida, codigoVisible, hashProfesor, estado = "ESPERANDO";
  public CrearPartidaDto configuracion;
  public Crucigrama crucigrama;
  public Map<String, Jugador> jugadores = new LinkedHashMap<>();
  public long fechaHoraInicio, fechaHoraFin, ultimaActividadProfesor;
  public boolean avisoTres, avisoUno;
  public int tension;
  public Map<String, String> correos = new LinkedHashMap<>();
}
