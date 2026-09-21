package com.crossed.crucigrama.controlador;

import com.crossed.crucigrama.generador.GeneradorCrucigrama;
import com.crossed.crucigrama.modelo.Concepto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crucigramas")
public class ControladorCrucigrama {
  @Autowired private GeneradorCrucigrama generador;

  public record Solicitud(
      @NotNull @Size(min = 5, max = 20) List<@Valid Concepto> conceptos,
      @Min(5) @Max(15) int cantidad) {}

  @PostMapping("/generar")
  public Object generar(@Valid @RequestBody Solicitud datos) {
    return generador.generar(datos.conceptos(), datos.cantidad());
  }
}
