package com.crossed.crucigrama.modelo;

import jakarta.validation.constraints.*;

public record Concepto(
    @NotBlank @Size(max = 25) String palabra,
    @NotBlank @Size(max = 1000) String definicion,
    @Size(max = 2000) String fragmentoFuente) {}
