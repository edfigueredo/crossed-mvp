package com.crossed.partida.servicio;

import com.crossed.crucigrama.generador.*;
import com.crossed.crucigrama.mapper.*;
import com.crossed.crucigrama.modelo.*;
import com.crossed.crucigrama.servicio.Normalizador;
import com.crossed.excepcion.modelo.ErrorNegocio;
import com.crossed.jugador.modelo.*;
import com.crossed.partida.dto.*;
import com.crossed.partida.modelo.*;
import com.crossed.partida.repositorio.*;
import com.crossed.ranking.servicio.*;
import com.crossed.tiempo_real.servicio.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ServicioPartida {
  @Autowired private RepositorioPartida repositorio;
  @Autowired private GeneradorCrucigrama generador;
  @Autowired private ServicioRanking ranking;
  @Autowired private ServicioEventos eventos;
  @Autowired private Clock reloj;

  public long ahora() {
    return reloj.millis();
  }

  public synchronized Map<String, Object> crear(CrearPartidaDto datos) {
    if (!Set.of(5, 8, 10, 12, 15).contains(datos.cantidadPalabrasSolicitada())
        || !Set.of(0, 1, 2, 3, 5).contains(datos.intentosPorPalabra()))
      throw new ErrorNegocio("DATOS_INVALIDOS", "Revisá cantidad de palabras e intentos.");
    Crucigrama c = generador.generar(datos.conceptos(), datos.cantidadPalabrasSolicitada());
    Partida p = new Partida();
    p.configuracion = datos;
    p.crucigrama = c;
    for (int i = 0; i < 100; i++) {
      p.codigoVisible = UUID.randomUUID().toString().substring(0, 5).toUpperCase(Locale.ROOT);
      try {
        repositorio.codigo(p.codigoVisible);
      } catch (ErrorNegocio e) {
        break;
      }
    }
    p.idPartida =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneOffset.UTC)
                .format(reloj.instant())
            + "-"
            + p.codigoVisible;
    String token = Credenciales.crear();
    p.hashProfesor = Credenciales.hash(token);
    p.ultimaActividadProfesor = ahora();
    repositorio.guardar(p);
    return Map.of(
        "idPartida",
        p.idPartida,
        "codigoVisible",
        p.codigoVisible,
        "token",
        token,
        "cantidadPalabrasGenerada",
        c.palabras().size());
  }

  private void profesor(Partida p, String token) {
    if (!Credenciales.coincide(token, p.hashProfesor))
      throw new ErrorNegocio("NO_AUTORIZADO", "Credencial de profesor inválida.", 403);
  }

  private Jugador jugador(Partida p, String id, String token) {
    Jugador j = p.jugadores.get(id);
    if (j == null || !Credenciales.coincide(token, j.hashToken))
      throw new ErrorNegocio(
          "JUGADOR_NO_ENCONTRADO", "La sesión ya no está activa. Registrate nuevamente.", 403);
    return j;
  }

  private Jugador alumno(Partida p, String token) {
    return p.jugadores.values().stream()
        .filter(j -> Credenciales.coincide(token, j.hashToken))
        .findFirst()
        .orElseThrow(() -> new ErrorNegocio("NO_AUTORIZADO", "Registrate para ingresar.", 403));
  }

  public synchronized void autorizarCanal(String id, String token, boolean privado) {
    Partida p = actualizar(id);
    if (privado) profesor(p, token);
    else if (!Credenciales.coincide(token, p.hashProfesor)) alumno(p, token);
  }

  public synchronized Map<String, Object> porCodigo(String codigo) {
    return publico(actualizar(repositorio.codigo(codigo)));
  }

  private Map<String, Object> publico(Partida p) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("idPartida", p.idPartida);
    m.put("codigoVisible", p.codigoVisible);
    m.put("nombre", p.configuracion.nombre());
    m.put("materia", p.configuracion.materia());
    m.put("idioma", p.configuracion.idioma());
    m.put("estado", p.estado);
    m.put("cantidadJugadores", p.jugadores.size());
    m.put("cantidadPalabrasGenerada", p.crucigrama.palabras().size());
    m.put("mostrarResultados", p.configuracion.mostrarResultados());
    m.put("tension", p.tension);
    return m;
  }

  public synchronized Map<String, Object> panel(String id, String token) {
    Partida p = actualizar(id);
    profesor(p, token);
    p.ultimaActividadProfesor = ahora();
    repositorio.guardar(p);
    Map<String, Object> m = publico(p);
    m.put("crucigrama", p.crucigrama);
    m.put("segundosRestantes", Math.max(0, (p.fechaHoraFin - ahora() + 999) / 1000));
    m.put("correos", new ArrayList<>(p.correos.values()));
    m.put(
        "jugadores",
        p.jugadores.values().stream()
            .map(
                j -> {
                  Map<String, Object> d = new LinkedHashMap<>();
                  d.put("idJugador", j.idJugador);
                  d.put("nombre", j.nombre);
                  d.put("pantallaActiva", j.pantallaActiva);
                  d.put("finalizo", j.finalizo);
                  d.put(
                      "puntaje",
                      j.finalizo || p.estado.equals("FINALIZADA")
                          ? ranking.puntaje(j, p.crucigrama.palabras().size())
                          : null);
                  d.put("tiempo", j.finalizo ? j.tiempoFinalizacion : null);
                  return d;
                })
            .toList());
    if (p.estado.equals("FINALIZADA"))
      m.put("ranking", ranking.calcular(p.jugadores.values(), p.crucigrama.palabras().size()));
    return m;
  }

  public synchronized Map<String, Object> registrar(String id, String nombre, String correo) {
    Partida p = actualizar(id);
    if (p.estado.equals("FINALIZADA"))
      throw new ErrorNegocio("PARTIDA_FINALIZADA", "La partida terminó.");
    if (p.jugadores.size() >= 60)
      throw new ErrorNegocio("LIMITE_JUGADORES_ALCANZADO", "La sala tiene 60 participantes.");
    if (p.jugadores.values().stream().anyMatch(j -> j.correo.equalsIgnoreCase(correo.trim())))
      throw new ErrorNegocio("JUGADOR_YA_REGISTRADO", "Ese correo ya tiene una sesión activa.");
    Jugador j = new Jugador();
    j.idJugador = UUID.randomUUID().toString();
    j.nombre = nombre.trim();
    j.correo = correo.trim();
    String token = Credenciales.crear();
    j.hashToken = Credenciales.hash(token);
    j.ultimaActividad = ahora();
    p.jugadores.put(j.idJugador, j);
    repositorio.guardar(p);
    eventos.emitir(id, "JUGADOR_INGRESO");
    return Map.of("idJugador", j.idJugador, "token", token);
  }

  public synchronized Map<String, Object> vistaAlumno(
      String id, String jid, String token, Boolean activa) {
    Partida p = actualizar(id);
    Jugador j = jugador(p, jid, token);
    j.ultimaActividad = ahora();
    if (activa != null && j.pantallaActiva != activa) {
      j.pantallaActiva = activa;
      eventos.emitir(id, activa ? "JUGADOR_VOLVIO_PANTALLA" : "JUGADOR_SALIO_PANTALLA");
    }
    repositorio.guardar(p);
    Map<String, Object> m = publico(p);
    if (!p.estado.equals("ESPERANDO")) m.put("crucigrama", CrucigramaMapper.alumno(p.crucigrama));
    m.put("progreso", j.progreso);
    m.put("finalizo", j.finalizo);
    if (p.estado.equals("FINALIZADA") && p.configuracion.mostrarResultados())
      m.put("ranking", ranking.calcular(p.jugadores.values(), p.crucigrama.palabras().size()));
    return m;
  }

  public synchronized void iniciar(String id, String token) {
    Partida p = actualizar(id);
    profesor(p, token);
    if (!p.estado.equals("ESPERANDO"))
      throw new ErrorNegocio("PARTIDA_NO_DISPONIBLE", "La partida ya comenzó.");
    p.estado = "EN_CURSO";
    p.fechaHoraInicio = ahora() + 3000;
    p.fechaHoraFin = p.fechaHoraInicio + p.configuracion.duracionMinutos() * 60000L;
    repositorio.guardar(p);
    eventos.emitir(id, "PARTIDA_INICIADA");
  }

  public synchronized void editar(String id, String token, List<Concepto> conceptos) {
    Partida p = actualizar(id);
    profesor(p, token);
    if (!p.estado.equals("ESPERANDO"))
      throw new ErrorNegocio("PARTIDA_NO_DISPONIBLE", "Solo podés editar antes del inicio.");
    List<String> antes = p.crucigrama.palabras().stream().map(Palabra::palabra).toList();
    List<String> nuevas =
        conceptos.stream().map(c -> Normalizador.normalizar(c.palabra())).toList();
    if (antes.equals(nuevas)) {
      List<Palabra> palabras = new ArrayList<>();
      for (int i = 0; i < antes.size(); i++) {
        Palabra a = p.crucigrama.palabras().get(i);
        palabras.add(
            new Palabra(
                a.idPalabra(),
                a.palabra(),
                conceptos.get(i).definicion(),
                a.filaInicial(),
                a.columnaInicial(),
                a.direccion()));
      }
      p.crucigrama = new Crucigrama(p.crucigrama.filas(), p.crucigrama.columnas(), palabras);
    } else
      p.crucigrama = generador.generar(conceptos, p.configuracion.cantidadPalabrasSolicitada());
    repositorio.guardar(p);
  }

  public synchronized Map<String, Object> responder(
      String id, String jid, String token, long palabraId, String respuesta) {
    Partida p = actualizar(id);
    Jugador j = jugador(p, jid, token);
    if (!p.estado.equals("EN_CURSO") || ahora() < p.fechaHoraInicio)
      throw new ErrorNegocio("PARTIDA_NO_DISPONIBLE", "No se reciben respuestas en este momento.");
    Palabra palabra =
        p.crucigrama.palabras().stream()
            .filter(a -> a.idPalabra() == palabraId)
            .findFirst()
            .orElseThrow(() -> new ErrorNegocio("DATOS_INVALIDOS", "Palabra inexistente."));
    ProgresoJugador progreso = j.progreso.computeIfAbsent(palabraId, k -> new ProgresoJugador());
    if (!progreso.bloqueada) {
      progreso.intentosRealizados++;
      progreso.fechaHoraRespuesta = ahora();
      progreso.correcta = palabra.palabra().equals(Normalizador.normalizar(respuesta));
      progreso.bloqueada =
          progreso.correcta
              || (p.configuracion.intentosPorPalabra() > 0
                  && progreso.intentosRealizados >= p.configuracion.intentosPorPalabra());
    }
    j.ultimaActividad = ahora();
    if (j.correctas() == p.crucigrama.palabras().size() && !j.finalizo) {
      j.finalizo = true;
      j.tiempoFinalizacion = ahora() - p.fechaHoraInicio;
      eventos.emitir(id, "JUGADOR_FINALIZO");
    }
    tension(p);
    if (!p.jugadores.isEmpty() && p.jugadores.values().stream().allMatch(a -> a.finalizo))
      cerrar(p);
    repositorio.guardar(p);
    return Map.of(
        "correcta",
        progreso.correcta,
        "bloqueada",
        progreso.bloqueada,
        "intentosRestantes",
        p.configuracion.intentosPorPalabra() == 0
            ? -1
            : Math.max(0, p.configuracion.intentosPorPalabra() - progreso.intentosRealizados),
        "puntajeActual",
        ranking.puntaje(j, p.crucigrama.palabras().size()),
        "cantidadCorrectas",
        j.correctas());
  }

  public synchronized void finalizar(String id, String token) {
    Partida p = actualizar(id);
    profesor(p, token);
    if (!p.estado.equals("FINALIZADA")) {
      cerrar(p);
      repositorio.guardar(p);
    }
  }

  private void cerrar(Partida p) {
    p.estado = "FINALIZADA";
    p.tension = 7;
    p.correos.put("profesor", "PENDIENTE");
    if (!p.configuracion.mostrarResultados())
      p.jugadores.values().forEach(j -> p.correos.put(j.idJugador, "PENDIENTE"));
    eventos.emitir(p.idPartida, "PARTIDA_FINALIZADA");
  }

  private void tension(Partida p) {
    int total = p.jugadores.size();
    double porcentaje =
        total == 0
            ? 0
            : 100.0 * p.jugadores.values().stream().filter(j -> j.finalizo).count() / total;
    double[] limites = {10, 25, 40, 55, 70, 82, 92};
    int objetivo = 0;
    while (objetivo < 7 && porcentaje > limites[objetivo]) objetivo++;
    if (total < 5) objetivo = Math.min(objetivo, p.tension + 1);
    p.tension = Math.max(p.tension, objetivo);
    if (p.avisoTres) p.tension = Math.max(p.tension, 5);
    if (p.avisoUno) p.tension = Math.max(p.tension, 6);
  }

  public synchronized Partida actualizar(String id) {
    Partida p = repositorio.buscar(id);
    boolean cambio = false;
    if (!p.estado.equals("FINALIZADA")) {
      Iterator<Jugador> iterador = p.jugadores.values().iterator();
      while (iterador.hasNext()) {
        Jugador j = iterador.next();
        if (ahora() - j.ultimaActividad > 15000) {
          iterador.remove();
          cambio = true;
          eventos.emitir(id, "JUGADOR_DESCONECTADO");
        }
      }
      if (p.estado.equals("EN_CURSO")) {
        long resta = p.fechaHoraFin - ahora();
        if (resta <= 0) {
          cerrar(p);
          cambio = true;
        } else {
          if (resta <= 180000 && !p.avisoTres) {
            p.avisoTres = true;
            cambio = true;
            eventos.emitir(id, "QUEDAN_TRES_MINUTOS");
          }
          if (resta <= 60000 && !p.avisoUno) {
            p.avisoUno = true;
            cambio = true;
            eventos.emitir(id, "QUEDA_UN_MINUTO");
          }
          if (!p.jugadores.isEmpty() && p.jugadores.values().stream().allMatch(j -> j.finalizo)) {
            cerrar(p);
            cambio = true;
          }
        }
      }
      if (!p.estado.equals("FINALIZADA")) tension(p);
    }
    if (cambio) repositorio.guardar(p);
    return p;
  }

  public synchronized void eliminar(String id, String token) {
    Partida p = repositorio.buscar(id);
    profesor(p, token);
    repositorio.eliminar(p);
    eventos.emitir(id, "PARTIDA_ELIMINADA");
  }

  public synchronized void salir(String id, String jid, String token) {
    Partida p = actualizar(id);
    jugador(p, jid, token);
    if (!p.estado.equals("FINALIZADA")) {
      p.jugadores.remove(jid);
      repositorio.guardar(p);
      eventos.emitir(id, "JUGADOR_DESCONECTADO");
    }
  }

  public synchronized Object resultados(String id, String token) {
    Partida p = actualizar(id);
    boolean prof = Credenciales.coincide(token, p.hashProfesor);
    if (!prof) alumno(p, token);
    if (!p.estado.equals("FINALIZADA") || (!prof && !p.configuracion.mostrarResultados()))
      throw new ErrorNegocio("NO_AUTORIZADO", "El ranking no está disponible.", 403);
    return ranking.calcular(p.jugadores.values(), p.crucigrama.palabras().size());
  }

  public synchronized void reintentarCorreos(String id, String token) {
    Partida p = repositorio.buscar(id);
    profesor(p, token);
    if (!p.estado.equals("FINALIZADA"))
      throw new ErrorNegocio("PARTIDA_NO_DISPONIBLE", "La partida aún no terminó.");
    p.correos.replaceAll((k, v) -> v.equals("ENVIADO") ? v : "PENDIENTE");
    repositorio.guardar(p);
  }

  public synchronized void estadoCorreo(String id, String destinatario, String estado) {
    try {
      Partida p = repositorio.buscar(id);
      p.correos.put(destinatario, estado);
      repositorio.guardar(p);
    } catch (ErrorNegocio ignorado) {
    }
  }

  public synchronized List<Partida> correosPendientes() {
    List<Partida> salida = new ArrayList<>();
    for (String id : repositorio.ids())
      try {
        Partida p = repositorio.buscar(id);
        if (p.estado.equals("FINALIZADA") && p.correos.containsValue("PENDIENTE")) salida.add(p);
      } catch (ErrorNegocio e) {
        repositorio.quitarIndice(id);
      }
    return salida;
  }

  @Scheduled(fixedDelay = 1000)
  public synchronized void revisar() {
    for (String id : repositorio.ids())
      try {
        actualizar(id);
      } catch (ErrorNegocio e) {
        repositorio.quitarIndice(id);
      }
  }
}
