package com.crossed.partida.controlador;

import com.crossed.crucigrama.modelo.Concepto;
import com.crossed.partida.dto.CrearPartidaDto;
import com.crossed.partida.servicio.ServicioPartida;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/partidas")
public class ControladorPartida {
  @Autowired private ServicioPartida servicio;

  @PostMapping
  public Object crear(@Valid @RequestBody CrearPartidaDto datos) {
    return servicio.crear(datos);
  }

  @GetMapping("/codigo/{codigo}")
  public Object codigo(@PathVariable String codigo) {
    return servicio.porCodigo(codigo);
  }

  @GetMapping("/{id}")
  public Object panel(@PathVariable String id, @RequestHeader("X-Token") String token) {
    return servicio.panel(id, token);
  }

  @PostMapping("/{id}/iniciar")
  public void iniciar(@PathVariable String id, @RequestHeader("X-Token") String token) {
    servicio.iniciar(id, token);
  }

  @PostMapping("/{id}/finalizar")
  public void finalizar(@PathVariable String id, @RequestHeader("X-Token") String token) {
    servicio.finalizar(id, token);
  }

  @PutMapping("/{id}/crucigrama")
  public void editar(
      @PathVariable String id,
      @RequestHeader("X-Token") String token,
      @Valid @RequestBody Edicion datos) {
    servicio.editar(id, token, datos.conceptos());
  }

  public record Edicion(@NotNull @Size(min = 5, max = 20) List<@Valid Concepto> conceptos) {}

  @DeleteMapping("/{id}")
  public void eliminar(@PathVariable String id, @RequestHeader("X-Token") String token) {
    servicio.eliminar(id, token);
  }

  @GetMapping("/{id}/ranking")
  public Object ranking(@PathVariable String id, @RequestHeader("X-Token") String token) {
    return servicio.resultados(id, token);
  }

  @PostMapping("/{id}/correos/reintentar")
  public void correos(@PathVariable String id, @RequestHeader("X-Token") String token) {
    servicio.reintentarCorreos(id, token);
  }
}
