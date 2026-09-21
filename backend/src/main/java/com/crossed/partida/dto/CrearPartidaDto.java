package com.crossed.partida.dto;

import com.crossed.crucigrama.modelo.Concepto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CrearPartidaDto(
    @NotBlank @Size(max = 100) String nombre,
    @NotBlank @Size(max = 100) String materia,
    @NotBlank @Size(max = 100) String nombreProfesor,
    @NotBlank @Email @Size(max = 254) String correoProfesor,
    @Pattern(regexp = "es|en") @NotNull String idioma,
    @Min(1) @Max(180) int duracionMinutos,
    boolean mostrarResultados,
    int intentosPorPalabra,
    int cantidadPalabrasSolicitada,
    @NotNull @Size(min = 5, max = 20) List<@Valid Concepto> conceptos) {}
