package com.crossed.contenido.extractor;

import com.crossed.excepcion.modelo.ErrorNegocio;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.regex.*;
import org.apache.tika.Tika;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ExtractorContenido {
  public String archivo(MultipartFile archivo) {
    if (archivo.isEmpty()) throw new ErrorNegocio("ARCHIVO_VACIO", "El archivo está vacío.");
    String nombre =
        Optional.ofNullable(archivo.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
    if (!nombre.matches(".*\\.(pdf|doc|docx|ppt|pptx|xls|xlsx|txt)$"))
      throw new ErrorNegocio("ARCHIVO_NO_SOPORTADO", "Usá PDF, Word, PowerPoint, Excel o TXT.");
    try (var entrada = archivo.getInputStream()) {
      Tika tika = new Tika();
      tika.setMaxStringLength(60000);
      return comprobar(tika.parseToString(entrada));
    } catch (Exception e) {
      throw new ErrorNegocio(
          "ARCHIVO_NO_PROCESABLE",
          "No se pudo extraer texto. Si es un escaneo, aplicá OCR o pegá el texto.");
    }
  }

  public String enlace(String url) {
    try {
      URI uri = URI.create(url);
      String host = Optional.ofNullable(uri.getHost()).orElse("");
      if (host.equals("docs.google.com")) {
        Matcher d =
            Pattern.compile("/(document|spreadsheets)/d/([a-zA-Z0-9_-]+)").matcher(uri.getPath());
        if (d.find())
          url =
              "https://docs.google.com/"
                  + d.group(1)
                  + "/d/"
                  + d.group(2)
                  + "/export?format="
                  + (d.group(1).equals("document") ? "txt" : "csv");
      }
      String pagina = descargar(url, 0);
      if (host.equals("youtu.be") || host.equals("www.youtube.com") || host.equals("youtube.com")) {
        Matcher pista =
            Pattern.compile(
                    "\"captionTracks\"\\s*:\\s*\\[.*?\"baseUrl\"\\s*:\\s*(\"(?:\\\\.|[^\"])*\")",
                    Pattern.DOTALL)
                .matcher(pagina);
        if (!pista.find())
          throw new ErrorNegocio(
              "TRANSCRIPCION_NO_DISPONIBLE",
              "No hay una transcripción pública accesible. Pegá la transcripción o subila como"
                  + " TXT.");
        String enlace = new ObjectMapper().readValue(pista.group(1), String.class);
        return comprobar(Jsoup.parse(descargar(enlace, 0)).text());
      }
      var documento = Jsoup.parse(pagina);
      documento.select("script,style,nav,footer").remove();
      return comprobar(documento.text());
    } catch (ErrorNegocio e) {
      throw e;
    } catch (Exception e) {
      throw new ErrorNegocio(
          "ENLACE_NO_ACCESIBLE",
          "El enlace debe ser público y contener texto accesible. Exportá el material o pegá el"
              + " texto.");
    }
  }

  private String descargar(String url, int saltos) throws Exception {
    if (saltos > 3) throw new IllegalArgumentException();
    URI uri = URI.create(url);
    if (!Set.of("http", "https").contains(uri.getScheme())
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || (uri.getPort() != -1 && uri.getPort() != 443 && uri.getPort() != 80))
      throw new IllegalArgumentException();
    var dns =
        new org.apache.hc.client5.http.SystemDefaultDnsResolver() {
          @Override
          public InetAddress[] resolve(String nombre) throws UnknownHostException {
            InetAddress[] ips = super.resolve(nombre);
            for (InetAddress ip : ips) {
              byte[] b = ip.getAddress();
              if (ip.isAnyLocalAddress()
                  || ip.isLoopbackAddress()
                  || ip.isLinkLocalAddress()
                  || ip.isSiteLocalAddress()
                  || ip.isMulticastAddress()
                  || (b.length == 16 && (b[0] & 0xfe) == 0xfc)
                  || (b.length == 4
                      && ((b[0] & 255) == 0
                          || (b[0] & 255) >= 224
                          || ((b[0] & 255) == 100 && (b[1] & 255) >= 64 && (b[1] & 255) <= 127))))
                throw new UnknownHostException("Dirección no pública");
            }
            return ips;
          }
        };
    var conexiones =
        org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder.create()
            .setDnsResolver(dns)
            .build();
    var configuracion =
        org.apache.hc.client5.http.config.RequestConfig.custom()
            .setConnectionRequestTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(8))
            .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(15))
            .build();
    try (var cliente =
        org.apache.hc.client5.http.impl.classic.HttpClients.custom()
            .setConnectionManager(conexiones)
            .setDefaultRequestConfig(configuracion)
            .disableRedirectHandling()
            .build()) {
      var pedido = new org.apache.hc.client5.http.classic.methods.HttpGet(uri);
      pedido.setHeader("User-Agent", "CrossEd/1.0");
      return cliente.execute(
          pedido,
          respuesta -> {
            if (respuesta.getCode() / 100 == 3) {
              var ubicacion = respuesta.getFirstHeader("location");
              if (ubicacion == null) throw new java.io.IOException();
              try {
                return descargar(uri.resolve(ubicacion.getValue()).toString(), saltos + 1);
              } catch (Exception e) {
                throw new java.io.IOException(e);
              }
            }
            if (respuesta.getCode() != 200 || respuesta.getEntity() == null)
              throw new java.io.IOException();
            try (var cuerpo = respuesta.getEntity().getContent()) {
              byte[] datos = cuerpo.readNBytes(2_000_001);
              if (datos.length > 2_000_000) throw new java.io.IOException();
              return new String(datos, java.nio.charset.StandardCharsets.UTF_8);
            }
          });
    }
  }

  public String comprobar(String texto) {
    if (texto == null || texto.trim().length() < 80)
      throw new ErrorNegocio(
          "ENLACE_SIN_CONTENIDO", "Se necesitan al menos 80 caracteres de texto legible.");
    if (texto.length() > 60000)
      throw new ErrorNegocio(
          "DATOS_INVALIDOS", "El material supera 60.000 caracteres. Dividilo en partes.");
    return texto.trim();
  }
}
