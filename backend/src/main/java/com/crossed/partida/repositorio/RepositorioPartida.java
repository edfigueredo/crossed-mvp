package com.crossed.partida.repositorio;

import com.crossed.excepcion.modelo.ErrorNegocio;
import com.crossed.partida.modelo.Partida;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioPartida {
  @Autowired private StringRedisTemplate redis;
  @Autowired private ObjectMapper json;

  public void guardar(Partida p) {
    try {
      redis
          .opsForValue()
          .set("crossed:partida:" + p.idPartida, json.writeValueAsString(p), Duration.ofHours(6));
      redis
          .opsForValue()
          .set("crossed:codigo:" + p.codigoVisible, p.idPartida, Duration.ofHours(6));
      redis.opsForSet().add("crossed:indice", p.idPartida);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public Partida buscar(String id) {
    String s = redis.opsForValue().get("crossed:partida:" + id);
    if (s == null)
      throw new ErrorNegocio("PARTIDA_NO_ENCONTRADA", "La partida no existe o expiró.", 404);
    try {
      return json.readValue(s, Partida.class);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String codigo(String codigo) {
    String id = redis.opsForValue().get("crossed:codigo:" + codigo.toUpperCase(Locale.ROOT));
    if (id == null)
      throw new ErrorNegocio("PARTIDA_NO_ENCONTRADA", "No encontramos ese código.", 404);
    return id;
  }

  public void eliminar(Partida p) {
    redis.delete(List.of("crossed:partida:" + p.idPartida, "crossed:codigo:" + p.codigoVisible));
    redis.opsForSet().remove("crossed:indice", p.idPartida);
  }

  public Set<String> ids() {
    Set<String> ids = redis.opsForSet().members("crossed:indice");
    return ids == null ? Set.of() : ids;
  }

  public void quitarIndice(String id) {
    redis.opsForSet().remove("crossed:indice", id);
  }
}
