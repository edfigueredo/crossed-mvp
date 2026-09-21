package com.crossed.crucigrama.dto;

import lombok.Builder;

@Builder
public record PalabraAlumnoDto(
    long idPalabra,
    String definicion,
    int filaInicial,
    int columnaInicial,
    String direccion,
    int cantidadLetras) {}
