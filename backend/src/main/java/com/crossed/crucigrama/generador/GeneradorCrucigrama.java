package com.crossed.crucigrama.generador;

import com.crossed.crucigrama.modelo.*;
import com.crossed.crucigrama.servicio.Normalizador;
import com.crossed.crucigrama.validador.*;
import com.crossed.excepcion.modelo.ErrorNegocio;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GeneradorCrucigrama {
  @Autowired private ValidadorCrucigrama validador;

  public Crucigrama generar(List<Concepto> conceptos, int cantidad) {
    Map<String, Concepto> unicos = new LinkedHashMap<>();
    for (Concepto c : conceptos) {
      String n = Normalizador.normalizar(c.palabra());
      if (n.length() > 1 && n.length() <= 25)
        unicos.putIfAbsent(n, new Concepto(n, c.definicion(), c.fragmentoFuente()));
    }
    List<Concepto> base = new ArrayList<>(unicos.values());
    base.sort(Comparator.comparingInt((Concepto c) -> c.palabra().length()).reversed());
    List<Palabra> mejor = new ArrayList<>();
    Random azar = new Random(base.hashCode());
    for (int intento = 0; intento < 100; intento++) {
      List<Concepto> orden = new ArrayList<>(base);
      if (intento > 0) Collections.shuffle(orden, azar);
      List<Palabra> puestas = new ArrayList<>();
      long id = 1;
      for (Concepto concepto : orden) {
        if (puestas.size() >= cantidad) break;
        String texto = concepto.palabra();
        Palabra elegida = null;
        double valor = -Double.MAX_VALUE;
        if (puestas.isEmpty())
          elegida =
              new Palabra(
                  id, texto, concepto.definicion(), 12, (25 - texto.length()) / 2, "HORIZONTAL");
        else
          for (Palabra existente : List.copyOf(puestas))
            for (int a = 0; a < existente.palabra().length(); a++)
              for (int b = 0; b < texto.length(); b++) {
                if (existente.palabra().charAt(a) != texto.charAt(b)) continue;
                boolean horizontal = !existente.horizontal();
                int f = existente.fila(a) - (horizontal ? 0 : b),
                    c = existente.columna(a) - (horizontal ? b : 0);
                Palabra candidata =
                    new Palabra(
                        id,
                        texto,
                        concepto.definicion(),
                        f,
                        c,
                        horizontal ? "HORIZONTAL" : "VERTICAL");
                List<Palabra> prueba = new ArrayList<>(puestas);
                prueba.add(candidata);
                if (!validador.validar(prueba, 25, 25)) continue;
                int cruces = 0;
                for (Palabra otra : puestas)
                  for (int i = 0; i < otra.palabra().length(); i++)
                    for (int k = 0; k < texto.length(); k++)
                      if (otra.fila(i) == candidata.fila(k)
                          && otra.columna(i) == candidata.columna(k)) cruces++;
                double puntuacion =
                    10 * cruces
                        + (b == 1
                            ? 5
                            : b == texto.length() - 2 ? 4 : b == texto.length() - 1 ? 3 : 0)
                        - area(prueba) * .08
                        - Math.abs(f - 12) * .05
                        - Math.abs(c - 12) * .05;
                if (puntuacion > valor) {
                  valor = puntuacion;
                  elegida = candidata;
                }
              }
        if (elegida != null) {
          puestas.add(elegida);
          id++;
        }
      }
      if (puestas.size() > mejor.size()) mejor = puestas;
      if (mejor.size() >= cantidad) break;
    }
    if (mejor.size() < 5)
      throw new ErrorNegocio(
          "CRUCIGRAMA_NO_GENERABLE",
          "Se necesitan al menos cinco conceptos conectables. Agregá o cambiá el contenido.");
    int f = mejor.stream().mapToInt(Palabra::filaInicial).min().orElse(0),
        c = mejor.stream().mapToInt(Palabra::columnaInicial).min().orElse(0);
    List<Palabra> recorte =
        mejor.stream()
            .map(
                p ->
                    new Palabra(
                        p.idPalabra(),
                        p.palabra(),
                        p.definicion(),
                        p.filaInicial() - f,
                        p.columnaInicial() - c,
                        p.direccion()))
            .toList();
    int
        filas =
            recorte.stream().mapToInt(p -> p.fila(p.palabra().length() - 1)).max().orElse(0) + 1,
        columnas =
            recorte.stream().mapToInt(p -> p.columna(p.palabra().length() - 1)).max().orElse(0) + 1;
    if (!validador.validar(recorte, filas, columnas))
      throw new IllegalStateException("Geometría inválida");
    return new Crucigrama(filas, columnas, recorte);
  }

  private int area(List<Palabra> ps) {
    int f = 25, c = 25, F = 0, C = 0;
    for (Palabra p : ps) {
      f = Math.min(f, p.filaInicial());
      c = Math.min(c, p.columnaInicial());
      F = Math.max(F, p.fila(p.palabra().length() - 1));
      C = Math.max(C, p.columna(p.palabra().length() - 1));
    }
    return (F - f + 1) * (C - c + 1);
  }
}
