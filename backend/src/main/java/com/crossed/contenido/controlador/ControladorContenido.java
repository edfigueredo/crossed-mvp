package com.crossed.contenido.controlador;

import com.crossed.contenido.extractor.ExtractorContenido;
import com.crossed.ia.servicio.ServicioIa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ControladorContenido {
  @Autowired private ExtractorContenido extractor;
  @Autowired private ServicioIa ia;

  public record Enlace(@NotBlank @Size(max = 2000) String url) {}

  public record Fuente(
      @NotBlank @Size(max = 60000) String texto,
      @Min(5) @Max(15) int cantidad,
      @Pattern(regexp = "es|en") @NotNull String idioma) {}

  @PostMapping("/contenido/archivo")
  public Object archivo(@RequestParam MultipartFile archivo) {
    return Map.of("texto", extractor.archivo(archivo));
  }

  @PostMapping("/contenido/enlace")
  public Object enlace(@Valid @RequestBody Enlace datos) {
    return Map.of("texto", extractor.enlace(datos.url()));
  }

  @PostMapping("/crucigramas/conceptos")
  public Object conceptos(@Valid @RequestBody Fuente datos) {
    return ia.conceptos(extractor.comprobar(datos.texto()), datos.cantidad(), datos.idioma());
  }
}
