package com.crossed.excepcion.modelo;

public class ErrorNegocio extends RuntimeException {
  public final String codigo;
  public final int estado;

  public ErrorNegocio(String codigo, String mensaje) {
    this(codigo, mensaje, 400);
  }

  public ErrorNegocio(String codigo, String mensaje, int estado) {
    super(mensaje);
    this.codigo = codigo;
    this.estado = estado;
  }
}
