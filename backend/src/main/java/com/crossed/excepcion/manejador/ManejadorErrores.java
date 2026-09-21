package com.crossed.excepcion.manejador;

import com.crossed.excepcion.modelo.ErrorNegocio;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ManejadorErrores {
  @ExceptionHandler(ErrorNegocio.class)
  public ResponseEntity<?> negocio(ErrorNegocio e) {
    return salida(e.estado, e.codigo, e.getMessage());
  }

  @ExceptionHandler({
    org.springframework.web.bind.MethodArgumentNotValidException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.multipart.MaxUploadSizeExceededException.class,
    org.springframework.web.bind.MissingRequestHeaderException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<?> datos(Exception e) {
    return salida(
        400, "DATOS_INVALIDOS", "Revisá los datos y el tamaño del archivo (máximo 10 MB).");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> interno(Exception e) {
    org.slf4j.LoggerFactory.getLogger(getClass())
        .error("Fallo interno: {}", e.getClass().getSimpleName());
    return salida(500, "ERROR_INTERNO", "No se pudo completar la operación. Intentá nuevamente.");
  }

  private ResponseEntity<?> salida(int estado, String codigo, String mensaje) {
    return ResponseEntity.status(estado)
        .body(Map.of("codigo", codigo, "mensaje", mensaje, "fechaHora", Instant.now().toString()));
  }
}
