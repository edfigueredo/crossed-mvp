package com.crossed.ranking.servicio;

import com.crossed.jugador.modelo.Jugador;
import com.crossed.ranking.dto.ResultadoRankingDto;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ServicioRanking {
  public double puntaje(Jugador j, int total) {
    return Math.round(j.correctas() * 10000.0 / total) / 100.0;
  }

  public List<ResultadoRankingDto> calcular(Collection<Jugador> jugadores, int total) {
    List<Jugador> lista = new ArrayList<>(jugadores);
    lista.sort(
        Comparator.comparingInt(Jugador::correctas)
            .reversed()
            .thenComparingLong(j -> j.finalizo ? j.tiempoFinalizacion : Long.MAX_VALUE));
    List<ResultadoRankingDto> salida = new ArrayList<>();
    int posicion = 1;
    for (int i = 0; i < lista.size(); i++) {
      Jugador j = lista.get(i);
      if (i > 0 && !empate(j, lista.get(i - 1))) posicion = i + 1;
      boolean empatado =
          (i > 0 && empate(j, lista.get(i - 1)))
              || (i + 1 < lista.size() && empate(j, lista.get(i + 1)));
      salida.add(
          ResultadoRankingDto.builder()
              .posicion(posicion)
              .nombre(j.nombre)
              .puntaje(puntaje(j, total))
              .tiempo(j.finalizo ? j.tiempoFinalizacion : null)
              .cantidadCorrectas(j.correctas())
              .finalizo(j.finalizo)
              .empatado(empatado)
              .build());
    }
    return salida;
  }

  private boolean empate(Jugador a, Jugador b) {
    return a.correctas() == b.correctas()
        && a.finalizo == b.finalizo
        && (!a.finalizo || a.tiempoFinalizacion == b.tiempoFinalizacion);
  }
}
