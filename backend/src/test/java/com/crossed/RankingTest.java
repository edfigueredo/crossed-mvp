package com.crossed;

import static org.junit.jupiter.api.Assertions.*;

import com.crossed.jugador.modelo.*;
import com.crossed.ranking.servicio.ServicioRanking;
import java.util.*;
import org.junit.jupiter.api.Test;

class RankingTest {
  Jugador jugador(String nombre, int correctas, boolean termino, long tiempo) {
    Jugador j = new Jugador();
    j.nombre = nombre;
    j.finalizo = termino;
    j.tiempoFinalizacion = tiempo;
    for (long i = 0; i < correctas; i++) {
      ProgresoJugador p = new ProgresoJugador();
      p.correcta = true;
      j.progreso.put(i, p);
    }
    return j;
  }

  @Test
  void empatesIncompletosSinUsarTiempo() {
    var filas =
        new ServicioRanking()
            .calcular(
                List.of(
                    jugador("Juan", 5, true, 2000),
                    jugador("Ana", 5, true, 1000),
                    jugador("Lucía", 4, false, 0),
                    jugador("Pedro", 3, false, 800),
                    jugador("Sofía", 3, false, 200)),
                5);
    assertEquals(List.of(1, 2, 3, 4, 4), filas.stream().map(f -> f.posicion()).toList());
    assertEquals("Ana", filas.getFirst().nombre());
    assertTrue(filas.getLast().empatado());
    assertNull(filas.getLast().tiempo());
    assertEquals(100, filas.getFirst().puntaje());
  }
}
