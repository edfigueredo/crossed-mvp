package com.crossed.crucigrama.validador;

import com.crossed.crucigrama.modelo.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ValidadorCrucigrama {
  public static int maximo(int longitud) {
    return longitud <= 4 ? 1 : longitud <= 7 ? 2 : 3;
  }

  public boolean validar(List<Palabra> palabras, int filas, int columnas) {
    if (palabras.isEmpty()) return false;
    Celda[][] matriz = new Celda[filas][columnas];
    Map<Long, Set<Long>> grafo = new HashMap<>();
    for (Palabra p : palabras) {
      if (p.palabra().isEmpty()
          || !Set.of("HORIZONTAL", "VERTICAL").contains(p.direccion())
          || grafo.put(p.idPalabra(), new HashSet<>()) != null) return false;
      for (int i = 0; i < p.palabra().length(); i++) {
        int f = p.fila(i), c = p.columna(i);
        if (f < 0 || c < 0 || f >= filas || c >= columnas) return false;
        Celda celda = matriz[f][c];
        if (celda == null) {
          celda = new Celda();
          matriz[f][c] = celda;
        }
        if (celda.letra != null && celda.letra != p.palabra().charAt(i)) return false;
        if (p.horizontal()) {
          if (celda.idPalabraHorizontal != null) return false;
          celda.idPalabraHorizontal = p.idPalabra();
        } else {
          if (celda.idPalabraVertical != null) return false;
          celda.idPalabraVertical = p.idPalabra();
        }
        celda.letra = p.palabra().charAt(i);
      }
    }
    for (int f = 0; f < filas; f++)
      for (int c = 0; c < columnas; c++) {
        Celda a = matriz[f][c];
        if (a == null) continue;
        if (a.idPalabraHorizontal != null && a.idPalabraVertical != null) {
          grafo.get(a.idPalabraHorizontal).add(a.idPalabraVertical);
          grafo.get(a.idPalabraVertical).add(a.idPalabraHorizontal);
        }
        if (c + 1 < columnas && matriz[f][c + 1] != null) {
          Celda b = matriz[f][c + 1];
          if (a.idPalabraHorizontal == null || !a.idPalabraHorizontal.equals(b.idPalabraHorizontal))
            return false;
        }
        if (f + 1 < filas && matriz[f + 1][c] != null) {
          Celda b = matriz[f + 1][c];
          if (a.idPalabraVertical == null || !a.idPalabraVertical.equals(b.idPalabraVertical))
            return false;
        }
      }
    for (Palabra p : palabras)
      if (grafo.get(p.idPalabra()).size() > maximo(p.palabra().length())) return false;
    Set<Long> visitados = new HashSet<>();
    Deque<Long> cola = new ArrayDeque<>();
    cola.add(palabras.getFirst().idPalabra());
    while (!cola.isEmpty()) {
      Long id = cola.remove();
      if (visitados.add(id)) cola.addAll(grafo.get(id));
    }
    return visitados.size() == palabras.size();
  }
}
