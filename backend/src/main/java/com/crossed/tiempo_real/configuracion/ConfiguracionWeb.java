package com.crossed.tiempo_real.configuracion;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {
  @Value("${crossed.origenes}")
  private String origenes;

  @Bean
  public Clock reloj() {
    return Clock.systemUTC();
  }

  @Override
  public void addCorsMappings(CorsRegistry registro) {
    registro
        .addMapping("/**")
        .allowedOrigins(origenes.split(","))
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("Content-Type", "X-Token");
  }
}
