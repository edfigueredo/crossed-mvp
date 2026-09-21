package com.crossed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CrossEdAplicacion {
  public static void main(String[] argumentos) {
    SpringApplication.run(CrossEdAplicacion.class, argumentos);
  }
}
