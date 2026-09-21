package com.crossed;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.crossed.correo.servicio.*;
import com.crossed.crucigrama.generador.*;
import com.crossed.crucigrama.validador.*;
import com.crossed.excepcion.modelo.ErrorNegocio;
import com.crossed.jugador.modelo.*;
import com.crossed.partida.dto.*;
import com.crossed.partida.modelo.*;
import com.crossed.partida.repositorio.*;
import com.crossed.partida.servicio.*;
import com.crossed.ranking.servicio.*;
import com.crossed.tiempo_real.servicio.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

class ReglasPartidaTest {
  ServicioPartida servicio;
  RepositorioPartida repo;
  ServicioEventos eventos;
  Clock reloj;
  long ahora = 1_000_000;
  Partida partida;
  String id, token;
  Map<String, Object> alumno;

  @BeforeEach
  void preparar() {
    servicio = new ServicioPartida();
    repo = mock(RepositorioPartida.class);
    eventos = mock(ServicioEventos.class);
    reloj = mock(Clock.class);
    when(reloj.millis()).thenAnswer(i -> ahora);
    when(reloj.instant()).thenAnswer(i -> Instant.ofEpochMilli(ahora));
    when(repo.codigo(anyString())).thenThrow(new ErrorNegocio("X", "X"));
    when(repo.buscar(anyString())).thenAnswer(i -> partida);
    when(repo.ids()).thenAnswer(i -> partida == null ? Set.of() : Set.of(id));
    doAnswer(
            i -> {
              partida = i.getArgument(0);
              return null;
            })
        .when(repo)
        .guardar(any());
    GeneradorCrucigrama g = new GeneradorCrucigrama();
    ReflectionTestUtils.setField(g, "validador", new ValidadorCrucigrama());
    ReflectionTestUtils.setField(servicio, "repositorio", repo);
    ReflectionTestUtils.setField(servicio, "generador", g);
    ReflectionTestUtils.setField(servicio, "ranking", new ServicioRanking());
    ReflectionTestUtils.setField(servicio, "eventos", eventos);
    ReflectionTestUtils.setField(servicio, "reloj", reloj);
    var creada =
        servicio.crear(
            new CrearPartidaDto(
                "Clase",
                "Docker",
                "Profesor",
                "p@example.test",
                "es",
                5,
                false,
                2,
                5,
                GeometriaTest.ejemplos()));
    id = (String) creada.get("idPartida");
    token = (String) creada.get("token");
    alumno = servicio.registrar(id, "Ana", "ana@example.test");
  }

  String jid() {
    return (String) alumno.get("idJugador");
  }

  String jt() {
    return (String) alumno.get("token");
  }

  @Test
  void alumnoNoEsProfesor() {
    assertThrows(ErrorNegocio.class, () -> servicio.finalizar(id, jt()));
    assertThrows(ErrorNegocio.class, () -> servicio.panel(id, jt()));
    assertThrows(ErrorNegocio.class, () -> servicio.autorizarCanal(id, jt(), true));
    assertDoesNotThrow(() -> servicio.autorizarCanal(id, jt(), false));
  }

  @Test
  void noPuedeResponderPorOtro() {
    var otro = servicio.registrar(id, "Beto", "b@example.test");
    assertThrows(
        ErrorNegocio.class,
        () -> servicio.responder(id, (String) otro.get("idJugador"), jt(), 1, "a"));
  }

  @Test
  void dtoAlumnoSinSolucionesNiTiempoNiCorreo() throws Exception {
    servicio.iniciar(id, token);
    String json =
        new ObjectMapper().writeValueAsString(servicio.vistaAlumno(id, jid(), jt(), true));
    for (var p : partida.crucigrama.palabras()) assertFalse(json.contains(p.palabra()));
    assertFalse(json.contains("fechaHoraFin"));
    assertFalse(json.contains("segundosRestantes"));
    assertFalse(json.contains("correo"));
    assertFalse(json.contains("hash"));
  }

  @Test
  void desconexionEliminaYReingresoNuevoSinReiniciarTiempo() {
    servicio.iniciar(id, token);
    long fin = partida.fechaHoraFin;
    String previo = jid();
    ahora += 16000;
    servicio.actualizar(id);
    assertTrue(partida.jugadores.isEmpty());
    assertThrows(ErrorNegocio.class, () -> servicio.vistaAlumno(id, previo, jt(), true));
    var nuevo = servicio.registrar(id, "Ana", "ana@example.test");
    assertNotEquals(previo, nuevo.get("idJugador"));
    assertEquals(fin, partida.fechaHoraFin);
    assertTrue(partida.jugadores.get(nuevo.get("idJugador")).progreso.isEmpty());
  }

  @Test
  void cambiarPestanaNoElimina() {
    servicio.vistaAlumno(id, jid(), jt(), false);
    ahora += 10000;
    servicio.vistaAlumno(id, jid(), jt(), false);
    ahora += 10000;
    servicio.vistaAlumno(id, jid(), jt(), true);
    assertEquals(1, partida.jugadores.size());
    assertTrue(partida.jugadores.get(jid()).pantallaActiva);
  }

  @Test
  void alertasUnaSolaVez() {
    servicio.iniciar(id, token);
    ahora = partida.fechaHoraFin - 180000;
    servicio.actualizar(id);
    servicio.actualizar(id);
    verify(eventos, times(1)).emitir(id, "QUEDAN_TRES_MINUTOS");
    assertTrue(partida.tension >= 5);
    ahora = partida.fechaHoraFin - 60000;
    servicio.actualizar(id);
    servicio.actualizar(id);
    verify(eventos, times(1)).emitir(id, "QUEDA_UN_MINUTO");
    assertTrue(partida.tension >= 6);
  }

  @Test
  void tiempoVencidoCierraYBloquea() {
    servicio.iniciar(id, token);
    ahora = partida.fechaHoraFin;
    servicio.actualizar(id);
    assertEquals("FINALIZADA", partida.estado);
    assertThrows(ErrorNegocio.class, () -> servicio.responder(id, jid(), jt(), 1, "a"));
    verify(eventos).emitir(id, "PARTIDA_FINALIZADA");
  }

  @Test
  void limiteSesentaYCorreoDuplicado() {
    assertThrows(ErrorNegocio.class, () -> servicio.registrar(id, "Ana2", "ANA@example.test"));
    for (int i = 1; i < 60; i++) servicio.registrar(id, "Alumno" + i, "a" + i + "@example.test");
    assertThrows(ErrorNegocio.class, () -> servicio.registrar(id, "Extra", "extra@example.test"));
  }

  @Test
  void intentosYPuntosAutoritativos() {
    servicio.iniciar(id, token);
    ahora += 4000;
    var uno = servicio.responder(id, jid(), jt(), 1, "ZZZZZZ");
    assertEquals(1, uno.get("intentosRestantes"));
    var dos = servicio.responder(id, jid(), jt(), 1, "ZZZZZZ");
    assertEquals(true, dos.get("bloqueada"));
    var tres =
        servicio.responder(id, jid(), jt(), 1, partida.crucigrama.palabras().getFirst().palabra());
    assertEquals(false, tres.get("correcta"));
    assertEquals(0.0, tres.get("puntajeActual"));
  }

  @Test
  void todosCompletanFinaliza() {
    servicio.iniciar(id, token);
    ahora += 4000;
    for (var p : partida.crucigrama.palabras())
      servicio.responder(id, jid(), jt(), p.idPalabra(), p.palabra());
    assertEquals("FINALIZADA", partida.estado);
    assertTrue(partida.jugadores.get(jid()).finalizo);
  }

  @Test
  void rankingPrivado() {
    assertThrows(ErrorNegocio.class, () -> servicio.resultados(id, jt()));
    servicio.finalizar(id, token);
    assertThrows(ErrorNegocio.class, () -> servicio.resultados(id, jt()));
    assertDoesNotThrow(() -> servicio.resultados(id, token));
  }

  @Test
  void falloCorreoNoImpideCierreYSePuedeReintentar() {
    servicio.finalizar(id, token);
    ServicioCorreo correos = new ServicioCorreo();
    ReflectionTestUtils.setField(correos, "partidas", servicio);
    ReflectionTestUtils.setField(correos, "ranking", new ServicioRanking());
    ReflectionTestUtils.setField(correos, "correo", mock(JavaMailSender.class));
    ReflectionTestUtils.setField(correos, "origen", "");
    ReflectionTestUtils.setField(correos, "host", "sin-configurar");
    correos.procesar();
    assertEquals("FINALIZADA", partida.estado);
    assertTrue(partida.correos.values().stream().allMatch(e -> e.equals("ERROR")));
    assertEquals(1, partida.jugadores.size());
    servicio.reintentarCorreos(id, token);
    assertTrue(partida.correos.values().stream().allMatch(e -> e.equals("PENDIENTE")));
  }

  @Test
  void credencialesNoSeGuardanEnClaro() {
    assertNotEquals(token, partida.hashProfesor);
    assertNotEquals(jt(), partida.jugadores.get(jid()).hashToken);
  }
}
