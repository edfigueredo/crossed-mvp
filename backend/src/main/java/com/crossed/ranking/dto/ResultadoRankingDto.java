package com.crossed.ranking.dto;

import lombok.Builder;

@Builder
public record ResultadoRankingDto(
    int posicion,
    String nombre,
    double puntaje,
    Long tiempo,
    int cantidadCorrectas,
    boolean finalizo,
    boolean empatado) {}
