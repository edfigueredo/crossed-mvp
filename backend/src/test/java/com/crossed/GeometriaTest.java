package com.crossed;

import static org.junit.jupiter.api.Assertions.*;

import com.crossed.crucigrama.generador.*;
import com.crossed.crucigrama.modelo.*;
import com.crossed.crucigrama.servicio.Normalizador;
import com.crossed.crucigrama.validador.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GeometriaTest {
  ValidadorCrucigrama validador = new ValidadorCrucigrama();

  Palabra p(long id, String s, int f, int c, String d) {
    return new Palabra(id, s, "Pista", f, c, d);
  }

  @Test
  void cruceMismaCelda() {
    assertTrue(
        validador.validar(
            List.of(p(1, "CASA", 3, 2, "HORIZONTAL"), p(2, "SAL", 3, 4, "VERTICAL")), 10, 10));
  }

  @Test
  void letrasDiferentes() {
    assertFalse(
        validador.validar(
            List.of(p(1, "CASA", 3, 2, "HORIZONTAL"), p(2, "SOL", 2, 4, "VERTICAL")), 10, 10));
  }

  @Test
  void terceraPalabra() {
    assertFalse(
        validador.validar(
            List.of(
                p(1, "CASA", 3, 2, "HORIZONTAL"),
                p(2, "SAL", 3, 4, "VERTICAL"),
                p(3, "SALA", 3, 4, "HORIZONTAL")),
            10,
            10));
  }

  @Test
  void contactosLaterales() {
    assertFalse(
        validador.validar(
            List.of(p(1, "CASA", 3, 2, "HORIZONTAL"), p(2, "SAL", 4, 2, "HORIZONTAL")), 10, 10));
  }

  @Test
  void antesYDespues() {
    assertFalse(
        validador.validar(
            List.of(p(1, "CASA", 3, 2, "HORIZONTAL"), p(2, "SAL", 3, 6, "HORIZONTAL")), 10, 10));
  }

  @Test
  void desconectado() {
    assertFalse(
        validador.validar(
            List.of(p(1, "CASA", 3, 2, "HORIZONTAL"), p(2, "SAL", 7, 2, "HORIZONTAL")), 10, 10));
  }

  @Test
  void fueraDeLimites() {
    assertFalse(validador.validar(List.of(p(1, "CASA", 9, 8, "HORIZONTAL")), 10, 10));
  }

  @Test
  void limitesPorLongitud() {
    assertEquals(1, ValidadorCrucigrama.maximo(4));
    assertEquals(2, ValidadorCrucigrama.maximo(5));
    assertEquals(2, ValidadorCrucigrama.maximo(7));
    assertEquals(3, ValidadorCrucigrama.maximo(8));
  }

  @Test
  void rechazaExcesoDeCruces() {
    assertFalse(
        validador.validar(
            List.of(
                p(1, "CASA", 3, 2, "HORIZONTAL"),
                p(2, "CAL", 3, 2, "VERTICAL"),
                p(3, "SAL", 3, 4, "VERTICAL")),
            10,
            10));
  }

  @Test
  void conservaEnie() {
    assertEquals("ARBOL", Normalizador.normalizar("árbol"));
    assertNotEquals(Normalizador.normalizar("año"), Normalizador.normalizar("ano"));
  }

  static List<Concepto> ejemplos() {
    return Arrays.stream(
            new String[] {
              "CONTENEDOR",
              "IMAGEN",
              "DOCKER",
              "VOLUMEN",
              "PUERTO",
              "RED",
              "SERVICIO",
              "REGISTRO",
              "CAPA",
              "COMPOSE",
              "VIRTUALIZACION",
              "DEPENDENCIA",
              "PROCESO",
              "SISTEMA",
              "TERMINAL"
            })
        .map(s -> new Concepto(s, "Pista para el ejercicio", ""))
        .toList();
  }

  @Test
  void generaRecortaValidaYEsReproducible() {
    GeneradorCrucigrama g = new GeneradorCrucigrama();
    ReflectionTestUtils.setField(g, "validador", validador);
    Crucigrama c = g.generar(ejemplos(), 10);
    assertTrue(c.palabras().size() >= 5);
    assertTrue(validador.validar(c.palabras(), c.filas(), c.columnas()));
    assertEquals(c, g.generar(ejemplos(), 10));
  }

  @Test
  void reduceCantidadSolicitada() {
    GeneradorCrucigrama g = new GeneradorCrucigrama();
    ReflectionTestUtils.setField(g, "validador", validador);
    Crucigrama base = g.generar(ejemplos(), 5);
    Crucigrama reducido =
        g.generar(
            base.palabras().stream()
                .map(p -> new Concepto(p.palabra(), p.definicion(), ""))
                .toList(),
            15);
    assertEquals(5, reducido.palabras().size());
  }

  @Test
  void menosDeCincoNoGenera() {
    GeneradorCrucigrama g = new GeneradorCrucigrama();
    ReflectionTestUtils.setField(g, "validador", validador);
    assertThrows(
        com.crossed.excepcion.modelo.ErrorNegocio.class,
        () ->
            g.generar(
                List.of(new Concepto("AAAA", "Pista", ""), new Concepto("BBBB", "Pista", "")), 5));
  }
}
