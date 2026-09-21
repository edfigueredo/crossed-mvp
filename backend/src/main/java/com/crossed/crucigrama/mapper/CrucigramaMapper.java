package com.crossed.crucigrama.mapper;

import com.crossed.crucigrama.dto.*;
import com.crossed.crucigrama.modelo.*;
import java.util.*;

public final class CrucigramaMapper {
  public static Map<String, Object> alumno(Crucigrama c) {
    return Map.of(
        "filas",
        c.filas(),
        "columnas",
        c.columnas(),
        "palabras",
        c.palabras().stream()
            .map(
                p ->
                    PalabraAlumnoDto.builder()
                        .idPalabra(p.idPalabra())
                        .definicion(p.definicion())
                        .filaInicial(p.filaInicial())
                        .columnaInicial(p.columnaInicial())
                        .direccion(p.direccion())
                        .cantidadLetras(p.palabra().length())
                        .build())
            .toList());
  }
}
