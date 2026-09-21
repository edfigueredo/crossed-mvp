package com.crossed.crucigrama.modelo;

public record Palabra(
    long idPalabra,
    String palabra,
    String definicion,
    int filaInicial,
    int columnaInicial,
    String direccion) {
  public boolean horizontal() {
    return direccion.equals("HORIZONTAL");
  }

  public int fila(int i) {
    return filaInicial + (horizontal() ? 0 : i);
  }

  public int columna(int i) {
    return columnaInicial + (horizontal() ? i : 0);
  }
}
