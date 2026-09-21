package com.crossed.crucigrama.servicio;

import java.text.Normalizer;
import java.util.Locale;

public final class Normalizador {
  private Normalizador() {}

  public static String normalizar(String texto) {
    return Normalizer.normalize(
            texto.toUpperCase(Locale.ROOT).replace("Ñ", "\uE000"), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replace("\uE000", "Ñ")
        .replaceAll("[^A-ZÑ]", "");
  }
}
