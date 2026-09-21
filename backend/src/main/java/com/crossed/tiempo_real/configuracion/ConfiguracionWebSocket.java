package com.crossed.tiempo_real.configuracion;

import com.crossed.partida.servicio.ServicioPartida;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class ConfiguracionWebSocket implements WebSocketMessageBrokerConfigurer {
  @Value("${crossed.origenes}")
  private String origenes;

  @Autowired @Lazy private ServicioPartida partidas;

  public void registerStompEndpoints(StompEndpointRegistry registro) {
    registro.addEndpoint("/ws").setAllowedOrigins(origenes.split(","));
  }

  public void configureMessageBroker(MessageBrokerRegistry registro) {
    registro.enableSimpleBroker("/tema");
    registro.setApplicationDestinationPrefixes("/accion");
  }

  public void configureWebSocketTransport(WebSocketTransportRegistration registro) {
    registro.setMessageSizeLimit(4096).setSendBufferSizeLimit(65536).setSendTimeLimit(10000);
  }

  public void configureClientInboundChannel(ChannelRegistration registro) {
    registro.interceptors(
        new ChannelInterceptor() {
          public Message<?> preSend(Message<?> mensaje, MessageChannel canal) {
            StompHeaderAccessor cabecera =
                MessageHeaderAccessor.getAccessor(mensaje, StompHeaderAccessor.class);
            if (cabecera == null) return mensaje;
            if (cabecera.getCommand() == StompCommand.CONNECT) {
              String token = cabecera.getFirstNativeHeader("X-Token");
              String id = cabecera.getFirstNativeHeader("Partida");
              partidas.autorizarCanal(id, token, false);
              cabecera.getSessionAttributes().put("token", token);
              cabecera.getSessionAttributes().put("partida", id);
            }
            if (cabecera.getCommand() == StompCommand.SUBSCRIBE) {
              String id = (String) cabecera.getSessionAttributes().get("partida"),
                  token = (String) cabecera.getSessionAttributes().get("token"),
                  destino = cabecera.getDestination();
              boolean profesor = ("/tema/profesor/" + id).equals(destino);
              if (!profesor && !("/tema/partida/" + id).equals(destino))
                throw new IllegalArgumentException("Canal no autorizado");
              partidas.autorizarCanal(id, token, profesor);
            }
            if (cabecera.getCommand() == StompCommand.SEND)
              throw new IllegalArgumentException("Usá REST para acciones");
            return mensaje;
          }
        });
  }
}
