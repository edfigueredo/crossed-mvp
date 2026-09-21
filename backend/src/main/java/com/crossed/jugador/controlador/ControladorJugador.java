package com.crossed.jugador.controlador;

import com.crossed.partida.servicio.ServicioPartida;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/partidas/{id}")
public class ControladorJugador {
  @Autowired private ServicioPartida servicio;

  public record Registro(
      @NotBlank @Size(max = 100) String nombre, @NotBlank @Email @Size(max = 254) String correo) {}

  public record Latido(boolean pantallaActiva) {}

  public record Respuesta(long idPalabra, @NotBlank @Size(max = 100) String respuesta) {}

  @PostMapping("/jugadores")
  public Object registrar(@PathVariable String id, @Valid @RequestBody Registro datos) {
    return servicio.registrar(id, datos.nombre(), datos.correo());
  }

  @GetMapping("/jugadores")
  public Object jugadores(@PathVariable String id, @RequestHeader("X-Token") String token) {
    return servicio.panel(id, token).get("jugadores");
  }

  @PostMapping("/jugadores/{jid}/heartbeat")
  public Object latido(
      @PathVariable String id,
      @PathVariable String jid,
      @RequestHeader("X-Token") String token,
      @RequestBody Latido datos) {
    return servicio.vistaAlumno(id, jid, token, datos.pantallaActiva());
  }

  @GetMapping("/crucigrama")
  public Object grilla(
      @PathVariable String id,
      @RequestParam String idJugador,
      @RequestHeader("X-Token") String token) {
    return servicio.vistaAlumno(id, idJugador, token, null);
  }

  @PostMapping("/jugadores/{jid}/respuestas")
  public Object responder(
      @PathVariable String id,
      @PathVariable String jid,
      @RequestHeader("X-Token") String token,
      @Valid @RequestBody Respuesta datos) {
    return servicio.responder(id, jid, token, datos.idPalabra(), datos.respuesta());
  }

  @DeleteMapping("/jugadores/{jid}")
  public void salir(
      @PathVariable String id, @PathVariable String jid, @RequestHeader("X-Token") String token) {
    servicio.salir(id, jid, token);
  }
}
