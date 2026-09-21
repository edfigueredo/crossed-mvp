package com.crossed.correo.servicio;

import com.crossed.jugador.modelo.Jugador;
import com.crossed.partida.modelo.Partida;
import com.crossed.partida.servicio.ServicioPartida;
import com.crossed.ranking.servicio.ServicioRanking;
import java.util.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ServicioCorreo {
  @Autowired private ServicioPartida partidas;
  @Autowired private ServicioRanking ranking;
  @Autowired private JavaMailSender correo;

  @Value("${crossed.mail-from:}")
  private String origen;

  @Value("${spring.mail.host:}")
  private String host;

  @Value("${crossed.mail-from-name:CrossEd}")
  private String nombreRemitente;

  @Value("${crossed.brevo-api-key:}")
  private String brevoApiKey;

  @Value("${crossed.brevo-api-url:https://api.brevo.com/v3/smtp/email}")
  private String brevoApiUrl;

  @Scheduled(fixedDelay = 5000)
  public void procesar() {
    for (Partida p : partidas.correosPendientes())
      for (var entrada : p.correos.entrySet())
        if (entrada.getValue().equals("PENDIENTE")) {
          String clave = entrada.getKey();
          try {
            if (origen == null || origen.isBlank()) throw new IllegalStateException();
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(origen);
            mensaje.setSubject("CrossEd · Resultados de " + p.configuracion.nombre());
            String encabezado =
                p.configuracion.nombre()
                    + " · "
                    + p.configuracion.materia()
                    + "\nInicio: "
                    + java.time.Instant.ofEpochMilli(p.fechaHoraInicio)
                    + "\nDuración configurada: "
                    + p.configuracion.duracionMinutos()
                    + " minutos\n";
            var resultados = ranking.calcular(p.jugadores.values(), p.crucigrama.palabras().size());
            if (clave.equals("profesor")) {
              mensaje.setTo(p.configuracion.correoProfesor());
              StringBuilder tabla =
                  new StringBuilder(encabezado)
                      .append("Participantes finales: ")
                      .append(p.jugadores.size())
                      .append("\nCompletaron: ")
                      .append(p.jugadores.values().stream().filter(j -> j.finalizo).count())
                      .append("\n\nPosición | Alumno | Puntaje | Tiempo (ms)\n");
              resultados.forEach(
                  f ->
                      tabla
                          .append(f.posicion())
                          .append(" | ")
                          .append(f.nombre())
                          .append(" | ")
                          .append(f.puntaje())
                          .append(" | ")
                          .append(f.tiempo() == null ? "-" : f.tiempo())
                          .append("\n"));
              mensaje.setText(tabla.toString());
            } else {
              Jugador j = p.jugadores.get(clave);
              mensaje.setTo(j.correo);
              var lista = new ArrayList<>(p.jugadores.values());
              lista.sort(
                  Comparator.comparingInt(Jugador::correctas)
                      .reversed()
                      .thenComparingLong(a -> a.finalizo ? a.tiempoFinalizacion : Long.MAX_VALUE));
              int indice = 0;
              while (!lista.get(indice).idJugador.equals(clave)) indice++;
              var resultado = resultados.get(indice);
              mensaje.setText(
                  encabezado
                      + "Puntaje: "
                      + resultado.puntaje()
                      + "\nPosición: "
                      + resultado.posicion()
                      + "\nCorrectas: "
                      + j.correctas()
                      + " / "
                      + p.crucigrama.palabras().size()
                      + "\n"
                      + (j.finalizo
                          ? "Tiempo (ms): " + j.tiempoFinalizacion
                          : "La partida finalizó antes de completar."));
            }
            enviar(mensaje);
            partidas.estadoCorreo(p.idPartida, clave, "ENVIADO");
          } catch (Exception e) {
            partidas.estadoCorreo(p.idPartida, clave, "ERROR");
          }
        }
  }

  private void enviar(SimpleMailMessage mensaje) {
    if (brevoApiKey != null && !brevoApiKey.isBlank()) {
      Map<String, Object> cuerpo =
          Map.of(
              "sender", Map.of("name", nombreRemitente, "email", origen),
              "to",
                  Arrays.stream(Objects.requireNonNull(mensaje.getTo()))
                      .map(destino -> Map.of("email", destino))
                      .toList(),
              "subject", Objects.requireNonNull(mensaje.getSubject()),
              "textContent", Objects.requireNonNull(mensaje.getText()));

      RestClient.create()
          .post()
          .uri(brevoApiUrl)
          .header("api-key", brevoApiKey)
          .contentType(MediaType.APPLICATION_JSON)
          .body(cuerpo)
          .retrieve()
          .toBodilessEntity();
      return;
    }

    if (host == null || host.isBlank() || host.equals("sin-configurar"))
      throw new IllegalStateException();
    correo.send(mensaje);
  }
}
