package com.crossed.partida.servicio;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public final class Credenciales {
  public static String crear() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static boolean coincide(String token, String hash) {
    return token != null
        && hash != null
        && MessageDigest.isEqual(
            hash(token).getBytes(StandardCharsets.UTF_8), hash.getBytes(StandardCharsets.UTF_8));
  }
}
