# CrossEd - Software Design Description (SDD)

**Version:** 1.1  
**Fecha:** 11/09/2026  
**Propósito:** documento maestro de diseño para construcción del MVP de CrossEd y ejemplo académico de SDD para estudiantes.

---

## 0. Instrucciones para ChatGPT Work

Este documento es la **fuente de verdad funcional y técnica** del MVP de CrossEd. Construir la aplicación completa respetando estas decisiones. Si aparece una ambigüedad menor, elegir la solución más simple, mantenible y coherente con el documento; si la ambigüedad puede cambiar una regla de negocio, dejarla documentada y pedir aprobación antes de modificar el comportamiento.

**Restricción de publicación:** construir, ejecutar y probar la primera versión, pero **no desplegarla públicamente ni publicar un enlace de producción hasta que el propietario del proyecto apruebe expresamente la versión**.

**Criterios de implementación:**

- Backend en Java 21 LTS + Spring Boot 3.x, Maven.
- Frontend web responsive en React / Next.js.
- Arquitectura de backend: monolito modular con capas dentro de cada módulo.
- Código de dominio, clases, métodos y variables propias del proyecto en español siempre que sea técnicamente razonable.
- Inyección de dependencias con `@Autowired` en campos.
- Mappers manuales usando Lombok Builder; no MapStruct.
- Comentarios breves y conceptuales; no comentar sintaxis obvia.
- Redis para estado temporal de partida; no base de datos persistente de usuarios en el MVP.
- REST para operaciones discretas y WebSocket + STOMP para eventos en tiempo real.
- Spring AI para integración con modelos de IA.
- OpenAI como proveedor principal y Google Gemini como respaldo.
- Nunca enviar respuestas correctas del crucigrama al frontend del alumno.
- El backend es autoritativo para tiempo, puntaje, intentos, progreso y estado de partida.

---

# 1. Visión del producto

CrossEd es una aplicación web educativa multijugador para generar y resolver crucigramas a partir de material de estudio.

El profesor suministra una fuente (texto, archivo o enlace), la IA identifica conceptos importantes con definiciones respaldadas por esa fuente, el sistema genera un crucigrama válido y el profesor inicia una partida. Los alumnos ingresan mediante enlace, QR o código, se registran con nombre y correo, y compiten en tiempo real.

El objetivo principal no es un examen de alta seguridad, sino una experiencia educativa lúdica, simple y rápida que familiarice a los alumnos con contenidos académicos.

---

# 2. Alcance del MVP

## 2.1 Roles

### Profesor

No posee cuenta ni login persistente. Crea una partida y recibe credenciales temporales para administrarla.

Puede:

- ingresar datos de la partida;
- cargar la fuente;
- generar conceptos mediante IA;
- revisar y editar palabras y definiciones;
- generar/reorganizar el crucigrama;
- iniciar y finalizar la partida;
- ver participantes conectados;
- ver quién terminó, puntaje y tiempo de finalización;
- recibir por correo el resultado final;
- crear un nuevo crucigrama, eliminando el estado anterior.

### Alumno

No posee cuenta persistente.

Puede:

- ingresar con enlace, QR o código;
- registrarse con nombre y correo;
- esperar el inicio;
- resolver el crucigrama;
- recibir mensajes visuales y humorísticos de presión;
- ver resultado final solo cuando la configuración de la partida lo permita;
- recibir su resultado por correo cuando el ranking público esté deshabilitado.

## 2.2 Capacidad

Máximo: **60 alumnos conectados por partida**.

## 2.3 Cantidad de palabras

Opciones del profesor:

- 5
- 8
- 10 (valor por defecto)
- 12
- 15

Mínimo: 5. Máximo: 15.

---

# 3. Stack tecnológico

## Backend

- Java 21 LTS
- Spring Boot 3.x
- Maven
- Spring Web
- Spring Validation
- Spring WebSocket + STOMP
- Spring AI
- Spring Data Redis
- Lombok
- librerías de extracción de PDF/Office según necesidad
- proveedor de correo transaccional o SMTP configurable

## Frontend

- React / Next.js
- TypeScript recomendado
- WebSocket/STOMP client
- CSS responsive, preferentemente sin animaciones pesadas

## Infraestructura

- Redis: estado temporal de partidas y jugadores.
- No se requiere PostgreSQL en el MVP.
- Producción: HTTPS y WSS.

---

# 4. Arquitectura general

Arquitectura: **monolito modular con arquitectura en capas dentro de cada módulo**.

Flujo interno típico:

`Controlador -> Servicio -> Repositorio/Modelo`

Los controladores no deben acceder directamente a repositorios de otros módulos. La comunicación entre módulos debe realizarse mediante servicios.

## 4.1 Módulos de backend

```text
com.crossed
├── partida
│   ├── controlador
│   ├── servicio
│   ├── modelo
│   ├── dto
│   ├── mapper
│   └── repositorio
├── jugador
│   ├── controlador
│   ├── servicio
│   ├── modelo
│   ├── dto
│   ├── mapper
│   └── repositorio
├── crucigrama
│   ├── controlador
│   ├── servicio
│   ├── modelo
│   ├── dto
│   ├── mapper
│   ├── generador
│   └── validador
├── ranking
│   ├── servicio
│   └── dto
├── contenido
│   ├── controlador
│   ├── servicio
│   ├── dto
│   └── extractor
├── ia
│   ├── servicio
│   ├── dto
│   └── configuracion
├── correo
│   └── servicio
├── tiempo_real
│   ├── configuracion
│   ├── dto
│   └── servicio
└── excepcion
    ├── modelo
    └── manejador
```

---

# 5. Modelo de dominio

## 5.1 Partida

Campos principales:

- `idPartida`
- `codigoVisible`
- `nombre`
- `materia`
- `nombreProfesor`
- `correoProfesor`
- `idioma`
- `duracionMinutos`
- `mostrarResultados`
- `intentosPorPalabra`
- `cantidadPalabrasSolicitada`
- `cantidadPalabrasGenerada`
- `fechaHoraInicio`
- `fechaHoraFin`
- `estado`
- credencial temporal de profesor (almacenada de forma segura)

`idPartida` se genera con fecha, hora y sufijo aleatorio corto. Ejemplo:

`20260909164215-A7K`

`codigoVisible` muestra solo el sufijo: `A7K`.

Estados:

- `CREADA`
- `ESPERANDO`
- `EN_CURSO`
- `FINALIZADA`

## 5.2 Jugador

- `idJugador`
- `nombre`
- `correo`
- `puntaje`
- `tiempoFinalizacion`
- `finalizo`
- `pantallaActiva`
- `cantidadCorrectas`
- `ultimaActividad`
- credencial temporal individual

## 5.3 Crucigrama

- `idCrucigrama`
- `filas`
- `columnas`
- `cantidadPalabras`
- `Celda[][] matriz`
- lista de palabras

## 5.4 Celda

Cada celda representa una posición física única en la matriz.

```java
private Character letra;
private boolean ocupadaHorizontal;
private boolean ocupadaVertical;
private Long idPalabraHorizontal;
private Long idPalabraVertical;
```

Una celda admite como máximo:

- una palabra horizontal;
- una palabra vertical.

Una intersección ya utilizada no puede recibir una tercera palabra.

## 5.5 Posicion

- `fila`
- `columna`

## 5.6 PalabraCrucigrama

- `idPalabra`
- `palabra`
- `definicion`
- `posicionInicial`
- `direccion`
- `puntaje`
- `cantidadCruces`

Dirección:

- `HORIZONTAL`
- `VERTICAL`

## 5.7 ProgresoJugador

- `id`
- `jugadorId`
- `palabraId`
- `intentosRealizados`
- `correcta`
- `bloqueada`
- `fechaHoraRespuesta`

---

# 6. Flujo del profesor

## 6.1 Formulario inicial

Campos:

- nombre de partida;
- materia;
- nombre del profesor;
- correo del profesor;
- idioma: Español / English;
- duración;
- mostrar resultados: Sí / No;
- intentos permitidos por palabra: 1, 2, 3, 5 o infinito; infinito por defecto;
- cantidad de palabras: 5, 8, 10, 12, 15.

## 6.2 Fuente

Debe permitir:

- pegar texto;
- subir archivo;
- pegar enlace.

Formatos de archivo v1:

- PDF
- Word
- PowerPoint
- Excel
- TXT

Enlaces previstos:

- Google Docs
- Google Sheets
- Canva
- Gamma
- Prezi
- YouTube
- páginas web

Los enlaces deben ser públicos o legibles sin autenticación adicional. Para YouTube se usa transcripción cuando exista.

## 6.3 Vista previa

El profesor ve:

- palabras seleccionadas;
- definiciones;
- crucigrama generado.

Puede:

- editar palabra;
- editar definición;
- eliminar concepto.

Cambio de definición: no requiere reorganizar geometría.

Cambio de palabra: obliga a regenerar y revalidar el crucigrama.

## 6.4 Inicio y administración

Al crear la partida se generan:

- `idPartida`
- `codigoVisible`
- QR
- enlace de ingreso
- token temporal del profesor

Panel durante la partida:

- nombre de partida;
- tiempo restante (solo profesor);
- conectados;
- cantidad que terminó;
- botón `Finalizar partida`;
- tabla:

`Alumno | Puntaje | Tiempo`

Mientras un jugador no termine, Puntaje y Tiempo pueden mostrarse como `-`.

Cuando un alumno cambia de pestaña/minimiza, su fila se muestra en rojo mientras `pantallaActiva=false`. Al regresar vuelve a estado normal. Esto es una señal visual, no una acusación de trampa.

---

# 7. Flujo del alumno

## 7.1 Ingreso

Enlace/QR dirige al registro.

Datos:

- nombre;
- correo.

No hay contraseña.

## 7.2 Sala de espera

Mostrar:

- nombre de partida;
- materia;
- cantidad actual de participantes;
- mensajes como `Ya somos 8 participantes`;
- animación pixel-art ligera de un personaje preparándose;
- mensaje `Esperando que el profesor inicie la partida...`.

## 7.3 Inicio

Al iniciar el profesor, todos reciben:

`3 ... 2 ... 1 ... ¡YAAAA!`

con estética arcade/pixel.

## 7.4 Juego

- crucigrama responsive;
- al seleccionar una palabra se destaca la definición correspondiente;
- PC: definiciones junto al tablero;
- móvil: mostrar principalmente la definición activa;
- correcta: revelar y bloquear palabra;
- incorrecta: feedback rojo, sin mostrar respuesta correcta;
- no hay penalización de puntaje por error;
- intentos limitados según configuración;
- si se agotan, esa palabra queda bloqueada;
- si intentos son infinitos, puede seguir probando hasta el fin de partida.

El alumno **no ve cronómetro ni tiempo restante**.

Recibe únicamente avisos especiales cuando faltan:

- 3 minutos;
- 1 minuto.

---

# 8. Tiempo de partida

El tiempo que importa es exclusivamente el controlado por el backend/profesor.

Al iniciar:

- guardar `fechaHoraInicio`;
- calcular `fechaHoraFin`.

El frontend del alumno no calcula ni muestra cuenta regresiva.

Eventos obligatorios:

- `QUEDAN_TRES_MINUTOS`
- `QUEDA_UN_MINUTO`
- `PARTIDA_FINALIZADA`

Cuando llega a cero:

1. bloquear nuevas respuestas;
2. calcular resultados;
3. calcular ranking;
4. intentar envío de correos;
5. mostrar pantalla final.

También puede terminar si:

- el profesor finaliza manualmente;
- todos los jugadores activos completan.

Finalización manual requiere confirmación:

`¿Seguro que querés finalizar la partida?`

Botones: `Cancelar` / `Finalizar partida`.

---

# 9. Desconexión y reingreso de alumnos

Regla del MVP:

> Si un alumno pierde conexión, cierra la pestaña, abandona o deja de responder, se elimina del listado activo y se elimina su progreso. Si vuelve, se registra como un jugador nuevo.

El tiempo no se reinicia: sigue siendo el tiempo global de la partida.

Ejemplo:

- partida: 20 min;
- alumno sale a los 7 min;
- vuelve a los 10 min;
- se registra nuevamente desde cero;
- la partida continúa con el tiempo global restante.

Al desconectarse se eliminan:

- respuestas;
- correctas;
- intentos;
- puntaje;
- token anterior;
- registro activo.

Al volver se crea:

- nuevo `idJugador`;
- nuevo token;
- progreso en cero.

Solo los jugadores activos al finalizar participan del ranking.

## 9.1 Heartbeat

Para detectar cortes reales:

- heartbeat aproximado cada 5 segundos;
- tolerancia aproximada de 15 segundos;
- si no hay actividad dentro de la tolerancia, eliminar jugador.

Un simple cambio de pestaña **no elimina** al jugador si sigue conectado; solo activa `pantallaActiva=false` y fila roja en panel del profesor.

---

# 10. Selección de conceptos mediante IA

La IA debe seleccionar palabras que sean académicamente relevantes y respaldadas por la fuente.

Priorizar:

- títulos;
- subtítulos;
- conceptos centrales;
- términos técnicos importantes;
- conceptos con definición explícita o claramente inferible desde el material.

Evitar:

- términos genéricos;
- palabras sin contexto;
- conceptos sin definición verificable en la fuente.

Cada candidato debe incluir idealmente:

- `palabra`
- `definicion`
- `fragmentoFuente` o evidencia textual

Para mejorar la geometría, solicitar a la IA aproximadamente `cantidadSolicitada + 5` candidatos. El generador selecciona el mejor subconjunto.

## 10.1 Estrategia de proveedores

1. OpenAI: llamada inicial.
2. Si falla o la salida no cumple estructura: un reintento corto.
3. Si sigue fallando: una llamada a Gemini.
4. Si ambos fallan: informar error recuperable al profesor.

Nunca ejecutar ambos en paralelo para la misma solicitud.

La IA no genera la geometría final. La geometría es responsabilidad del algoritmo determinístico y su validador.

---

# 11. Generación del crucigrama

## 11.1 Representación

Usar una matriz interna amplia, por ejemplo 25x25, y recortar filas/columnas vacías al final antes de enviar al frontend.

## 11.2 Algoritmo general

1. normalizar palabras;
2. ordenar aproximadamente de mayor a menor longitud;
3. colocar la primera palabra cerca del centro, horizontal;
4. para cada palabra restante, buscar letras comunes con palabras ya colocadas;
5. generar todas las posiciones candidatas;
6. validar cada candidato;
7. puntuar candidatos válidos;
8. elegir la mejor posición;
9. repetir;
10. ejecutar validador final independiente;
11. recortar matriz;
12. convertir a DTO.

## 11.3 Límites de cruces por longitud

- 1 a 4 letras: máximo 1 cruce.
- 5 a 7 letras: máximo 2 cruces.
- 8 o más letras: máximo 3 cruces.

Cada nueva intersección incrementa el contador de ambas palabras involucradas.

## 11.4 Preferencia de posición de cruce

No es una obligación geométrica, sino una preferencia del evaluador.

Favorecer que la palabra nueva cruce por:

1. segunda letra;
2. penúltima letra;
3. última letra.

Si esas posiciones impiden construir un tablero válido, se permite otro cruce.

## 11.5 Reglas estrictas del validador

Rechazar una ubicación si ocurre cualquiera de estas condiciones:

- dos letras diferentes intentan ocupar la misma celda;
- se intenta usar una dirección ya ocupada en esa celda;
- una intersección horizontal+vertical ya completa intenta recibir una tercera palabra;
- existe superposición paralela parcial o total no permitida;
- la palabra toca lateralmente otra palabra sin ser una intersección válida;
- se forman falsos cruces o palabras accidentales;
- las celdas inmediatamente antes o después del inicio/fin están ocupadas de forma inválida;
- la palabra queda fuera de límites;
- se supera el máximo de cruces de alguna palabra;
- la estructura queda desconectada.

Todas las palabras deben formar **un único componente conectado**.

## 11.6 Evaluación de candidatos

Puntaje orientativo:

- +10 por cruce válido;
- +5 si el cruce ocurre en la segunda letra de la palabra nueva;
- +4 en penúltima letra;
- +3 en última letra;
- +5 por cercanía al centro;
- +3 por mantener tablero compacto;
- +2 por equilibrar horizontal/vertical;
- penalización si aumenta demasiado el bounding box.

Los conflictos son rechazo directo, no penalización.

## 11.7 Intentos y reducción

Usar un límite acotado de intentos de organización (por ejemplo 100).

Si no se puede construir con N palabras:

`N -> N-1 -> N-2 ...`

hasta encontrar el máximo conjunto válido.

Si quedan menos de 5 palabras conectables, no crear partida y solicitar más/otro contenido.

## 11.8 Normalización de respuestas

Recomendado:

- comparación sin distinguir mayúsculas/minúsculas;
- ignorar tildes (`Á` equivalente a `A`, etc.);
- mantener `Ñ` distinta de `N`;
- mostrar definiciones con ortografía original;
- grilla en mayúsculas normalizadas.

---

# 12. Puntaje y ranking

Puntaje máximo: 100.

El puntaje se calcula en backend a partir de la cantidad de respuestas correctas respecto del total. Se puede normalizar al final para evitar acumulación de errores por redondeo.

Los errores no restan puntos.

## 12.1 Ranking

### Jugadores que completaron

Ordenar por:

1. puntaje descendente;
2. tiempo de finalización ascendente.

En la práctica, quienes completan deberían alcanzar 100 y el tiempo define el orden.

### Jugadores que no completaron

Ordenar por:

1. cantidad de correctas / puntaje descendente.

El tiempo **no desempata** a quienes no completaron.

Los empates conservan misma posición.

Ejemplo:

```text
1 Ana    100  04:10
2 Juan   100  05:20
3 Lucía   80  -
4 Pedro   70  -
4 Sofía   70  -
```

---

# 13. Tiempo real

Principio:

- REST = realizar una operación.
- WebSocket = notificar que algo ocurrió.

No usar WebSocket como único mecanismo de persistencia de respuestas.

Canales conceptuales:

- `/tema/partida/{idPartida}`
- `/tema/profesor/{idPartida}`

Eventos mínimos:

- `JUGADOR_INGRESO`
- `JUGADOR_DESCONECTADO`
- `JUGADOR_SALIO_PANTALLA`
- `JUGADOR_VOLVIO_PANTALLA`
- `PARTIDA_INICIADA`
- `JUGADOR_FINALIZO`
- `QUEDAN_TRES_MINUTOS`
- `QUEDA_UN_MINUTO`
- `PARTIDA_FINALIZADA`

La información privada del panel del profesor no debe exponerse a alumnos.

---

# 14. Seguridad

No hay login tradicional, pero sí credenciales temporales.

## 14.1 Profesor

Al crear partida, generar un token secreto temporal del profesor.

Debe ser requerido para:

- iniciar partida;
- finalizar partida;
- consultar panel privado;
- modificar crucigrama;
- reenviar correos;
- eliminar partida.

## 14.2 Alumno

Al registrarse, generar:

- `idJugador`;
- token temporal individual.

Debe utilizarse al enviar respuestas u otras acciones privadas.

El backend valida:

- token válido;
- jugador pertenece a partida;
- partida está en estado adecuado.

## 14.3 Respuestas correctas

**Nunca** incluir la palabra correcta en el DTO de alumno.

DTO alumno debe incluir únicamente:

- `idPalabra`
- `definicion`
- posición;
- dirección;
- cantidad de letras;
- puntaje/valor si corresponde.

La comparación se realiza exclusivamente en backend.

## 14.4 Datos calculados

El alumno nunca envía valores autoritativos de:

- puntaje;
- correctas;
- tiempo;
- finalización;
- intentos restantes.

Envía solo acciones, por ejemplo una respuesta; el servidor calcula el resto.

## 14.5 Secretos

No incluir en repositorio:

- API keys;
- claves SMTP;
- tokens de sesión;
- secretos de infraestructura.

Usar variables de entorno.

No imprimir secretos en logs.

---

# 15. Manejo de errores

Usar `@RestControllerAdvice` con un formato de error estándar.

Ejemplo conceptual:

```json
{
  "codigo": "ARCHIVO_NO_SOPORTADO",
  "mensaje": "El tipo de archivo seleccionado no es compatible.",
  "fechaHora": "2026-09-10T20:00:00"
}
```

Principio:

> El usuario recibe un mensaje comprensible; el detalle técnico queda en logs.

## 15.1 Errores previstos

- `ARCHIVO_NO_SOPORTADO`
- `ARCHIVO_VACIO`
- `ARCHIVO_NO_PROCESABLE`
- `ENLACE_NO_ACCESIBLE`
- `ENLACE_SIN_CONTENIDO`
- `TRANSCRIPCION_NO_DISPONIBLE`
- `IA_NO_DISPONIBLE`
- `RESPUESTA_IA_INVALIDA`
- `CRUCIGRAMA_NO_GENERABLE`
- `PARTIDA_NO_ENCONTRADA`
- `PARTIDA_FINALIZADA`
- `PARTIDA_NO_DISPONIBLE`
- `LIMITE_JUGADORES_ALCANZADO`
- `JUGADOR_YA_REGISTRADO` (mientras siga activo)
- `JUGADOR_NO_ENCONTRADO`
- `CORREO_NO_ENVIADO`
- `ERROR_INTERNO`

## 15.2 Advertencias no fatales

Ejemplo: se solicitaron 15 palabras y solo existen 11 conceptos válidos.

Mostrar advertencia y continuar con 11.

Si la geometría solo admite 9, informar y continuar con 9.

## 15.3 Fallo de correo

No debe impedir finalizar la partida.

Conservar temporalmente los resultados y permitir reintento de envío antes de eliminar la sesión.

---

# 16. Correo

## 16.1 Profesor

Siempre recibe un correo al finalizar con:

- nombre de partida;
- materia;
- fecha;
- hora de inicio;
- duración configurada;
- cantidad de participantes finales;
- ranking final: posición, nombre, puntaje, tiempo;
- promedio opcional;
- mejor puntaje opcional;
- cantidad que completó.

No incluir correos de alumnos en la tabla.

No adjuntar CSV en el MVP.

## 16.2 Alumno

Solo recibe correo cuando `mostrarResultados = false`.

Contenido:

- nombre de partida;
- materia;
- puntaje;
- posición final;
- correctas / total;
- tiempo si terminó;
- estado indicando que la partida finalizó antes de completar, si corresponde.

---

# 17. Cierre y limpieza de sesión

Al finalizar una partida:

1. bloquear respuestas;
2. calcular ranking;
3. intentar correos;
4. mostrar estado procesado;
5. conservar información temporal.

Pantalla del profesor:

`¡Crucigrama finalizado!`

`Los resultados fueron procesados y los correos enviados correctamente.`

Botón principal: `NUEVO CRUCIGRAMA`.

Al pulsarlo:

- eliminar estado temporal de la partida;
- limpiar tokens;
- volver al formulario inicial.

Como el cierre del navegador no es 100% detectable, usar TTL de Redis como red de seguridad y heartbeat del profesor. `sendBeacon` puede utilizarse como intento de limpieza rápida, pero no debe ser la única garantía.

---

# 18. Diseño visual del alumno

Estética:

- pixel-art/arcade de los 90;
- ligera;
- amigable;
- humorística;
- responsive;
- no copiar personajes ni assets con copyright.

La tensión se comunica sin mostrar ranking ni cronómetro.

## 18.1 Conejito indicador de tensión

Estados oficiales:

1. `CONTENTO`
2. `SONRIENTE`
3. `NEUTRO`
4. `SERIO`
5. `SUENO`
6. `PREOCUPADO`
7. `MIEDO`
8. `SE_FUE`

Los estados nunca retroceden durante una partida.

### Evolución por porcentaje que terminó

- 0-10%: Contento
- >10-25%: Sonriente
- >25-40%: Neutro
- >40-55%: Serio
- >55-70%: Sueño
- >70-82%: Preocupado
- >82-92%: Miedo
- >92%: Se fue

Para partidas muy pequeñas, suavizar los saltos para evitar cambios excesivos por un único jugador.

### Prioridad por tiempo

- faltan 3 minutos: estado mínimo `PREOCUPADO`;
- falta 1 minuto: estado mínimo `MIEDO`;
- fin: `SE_FUE`.

## 18.2 Ambiente

- Contento: día claro, sol, pájaros.
- Sonriente: día.
- Neutro: menos luz.
- Serio: atardecer.
- Sueño: puesta de sol.
- Preocupado: noche.
- Miedo: noche oscura + ojos.
- Se fue: conejo desaparece, noche + ojos.

## 18.3 Ojitos

Estilo de pares de ojos rojos simples, flotando en fondo oscuro.

- Contento: 0
- Sonriente: 0
- Neutro: 0
- Serio: 0
- Sueño: 1 par ocasional
- Preocupado: 2-3 pares
- Miedo: 4-7 pares
- Se fue: varios pares

Animación mínima: aparecer, parpadear, desaparecer o leve desplazamiento.

## 18.4 Mensajes humorísticos

Separar por fase y no saturar. Intervalo recomendado entre mensajes aleatorios: 30 segundos aprox. Los avisos de 3 y 1 minuto tienen prioridad y no respetan ese intervalo.

### Primeras fases

- `Los inteligentes ya entregaron.`
- `Cómo se nota quién estudió 👀`
- `Cada vez quedan menos...`
- `No hay más sol 🌙`

### Presión media

- `En cualquier momento tocan el himno.`
- `El portero pregunta si tenés llave 🔑`

### Presión alta

- `Dicen que apagues la luz cuando salgas 😅`
- `Menos mal que no sos conductor de ambulancia 🚑`
- `Hasta el conejo se fue...`

Los mensajes deben ser cargadas humorísticas, nunca insultos ni humillaciones.

---

# 19. Branding y créditos

Nombre: **CrossEd**.

Logo: icono minimalista de crucigrama con azul y amarillo, palabra `CrossEd`.

En las pantallas mostrar un crédito rotativo:

`[ROL HUMORÍSTICO] Eduardo Figueredo`

Roles posibles:

- Creador
- Arquitecto
- Ideólogo
- Visionario
- Máximo Artífice
- Gran Hacedor
- Domador de Ideas
- Jefe de Inventos
- Cerebro de la Operación
- Cebador de Mate
- Cadete
- Programador
- DevOps
- Tester Oficial
- Apagador de Incendios
- Reiniciador de Servidores
- Generador de Bugs
- Cazador de Bugs
- Arquitecto de Excusas
- Soporte Nivel Dios
- Maestro del Ctrl+Z
- Señor del Commit
- Guardián del Deploy
- Técnico del `En mi máquina funciona`

Rotación cada pocos segundos.

---

# 20. DTOs principales

## CrearPartidaDto

- nombre
- materia
- nombreProfesor
- correoProfesor
- idioma
- duracionMinutos
- mostrarResultados
- intentosPorPalabra
- cantidadPalabrasSolicitada

## PartidaDto

- idPartida
- codigoVisible
- nombre
- materia
- nombreProfesor
- idioma
- duracionMinutos
- mostrarResultados
- cantidadPalabrasGenerada
- estado
- cantidadJugadores

No exponer correo del profesor al alumno.

## RegistrarJugadorDto

- nombre
- correo

## JugadorDto

- idJugador
- nombre
- puntaje
- tiempoFinalizacion
- cantidadCorrectas
- finalizo
- pantallaActiva

No exponer correo a otros jugadores.

## PalabraCrucigramaDto (alumno)

- idPalabra
- definicion
- filaInicial
- columnaInicial
- direccion
- cantidadLetras
- puntaje

**Sin palabra correcta.**

## PalabraVistaPreviaDto (profesor)

- idPalabra
- palabra
- definicion
- filaInicial
- columnaInicial
- direccion

## RespuestaJugadorDto

- idPalabra
- respuesta

## ResultadoRespuestaDto

- correcta
- intentosRestantes
- bloqueada
- puntajeActual
- cantidadCorrectas

## ResultadoRankingDto

- posicion
- nombre
- puntaje
- tiempo
- cantidadCorrectas
- finalizo
- empatado

---

# 21. API REST propuesta

## Partida

- `POST /api/partidas`
- `GET /api/partidas/codigo/{codigoVisible}`
- `GET /api/partidas/{idPartida}` (profesor protegido)
- `POST /api/partidas/{idPartida}/iniciar`
- `POST /api/partidas/{idPartida}/finalizar`
- `DELETE /api/partidas/{idPartida}`

## Jugador

- `POST /api/partidas/{idPartida}/jugadores`
- `GET /api/partidas/{idPartida}/jugadores` (profesor protegido)

## Crucigrama

- `POST /api/crucigramas/conceptos`
- `POST /api/crucigramas/generar`
- `PUT /api/crucigramas/{idCrucigrama}`
- `GET /api/partidas/{idPartida}/crucigrama` (vista segura de alumno)
- `POST /api/partidas/{idPartida}/jugadores/{idJugador}/respuestas`

## Ranking

- `GET /api/partidas/{idPartida}/ranking`

Acceso de alumno solo después de finalizar y si `mostrarResultados=true`.

---

# 22. Estructura frontend propuesta

```text
frontend/
├── app/
│   ├── profesor/
│   │   ├── crear/
│   │   ├── previa/
│   │   └── partida/
│   ├── jugar/
│   │   ├── registro/
│   │   ├── espera/
│   │   ├── crucigrama/
│   │   └── resultado/
│   └── codigo/
├── componentes/
│   ├── Crucigrama/
│   ├── Conejo/
│   ├── Ambiente/
│   ├── MensajePresion/
│   ├── CuentaInicio/
│   ├── TablaProfesor/
│   └── Creditos/
├── servicios/
│   ├── api.ts
│   └── websocket.ts
└── tipos/
```

La representación enviada al frontend debe derivar de la matriz ya validada y recortada. El frontend no reinterpreta reglas geométricas; solo renderiza el resultado certificado por backend.

---

# 23. Requisitos no funcionales

- interfaz responsive para celular, tablet y PC;
- animaciones ligeras;
- soportar 60 jugadores sin degradación importante;
- no perder estado de partida por fallos secundarios como correo;
- logs técnicos sin secretos;
- mensajes de error amigables;
- no exponer respuestas correctas;
- tiempo controlado por servidor;
- código mantenible y didáctico;
- pruebas unitarias para generador/validador y reglas de ranking;
- pruebas de integración para flujo principal.

---

# 24. Casos de prueba críticos

## Generador

1. Dos palabras solo se cruzan si comparten exactamente la misma letra en la misma celda.
2. Una celda de cruce horizontal+vertical no admite tercera palabra.
3. Rechazar contactos laterales que formen falsas palabras.
4. Respetar máximo de cruces según longitud.
5. Todas las palabras quedan conectadas.
6. Si no entra la cantidad solicitada, reducir hasta el máximo válido.
7. No generar con menos de 5 palabras.

## Seguridad

1. Alumno no puede finalizar partida.
2. Alumno no puede ver DTO de profesor.
3. Alumno no puede responder por otro jugador.
4. Respuesta correcta nunca aparece en payload del alumno.
5. Ranking no disponible durante juego.

## Tiempo y desconexión

1. Alumno no ve cronómetro.
2. Todos reciben alerta a 3 min y 1 min.
3. Desconectado > tolerancia se elimina.
4. Si vuelve, entra desde cero como jugador nuevo.
5. El tiempo global no se reinicia.

## Correo

1. Fallo de correo no impide finalizar.
2. Datos permanecen para reintento.

---

# 25. Criterios de aceptación del MVP

El MVP se considera listo para revisión cuando:

- profesor puede crear partida desde una fuente válida;
- IA propone conceptos con definiciones;
- se genera un crucigrama que pasa todas las reglas del validador;
- profesor puede editar y regenerar;
- QR/enlace/código permiten ingreso;
- hasta 60 alumnos pueden registrarse;
- profesor inicia y todos reciben cuenta 3-2-1;
- respuestas se validan exclusivamente en backend;
- puntaje e intentos funcionan;
- panel profesor se actualiza en tiempo real;
- desconexiones eliminan jugador después de tolerancia;
- reingreso crea jugador nuevo sin reiniciar tiempo;
- avisos de 3 y 1 minuto funcionan;
- conejo/ambiente evolucionan según reglas;
- partida finaliza por tiempo, profesor o todos completados;
- ranking respeta reglas;
- correos se procesan según configuración;
- `Nuevo crucigrama` limpia la sesión;
- no se despliega públicamente hasta aprobación.

---

# 26. Entregables esperados de Work

1. Repositorio/proyecto completo con frontend y backend.
2. README con instalación local.
3. `.env.example` sin secretos.
4. Configuración de Redis para desarrollo.
5. Datos/mock simples para probar sin depender siempre de IA.
6. Pruebas unitarias del generador y validador.
7. Pruebas de ranking y tiempo.
8. Instrucciones para ejecutar todo localmente.
9. Capturas o evidencia de pruebas principales.
10. Lista breve de decisiones o desviaciones realizadas durante la implementación.
11. No publicar en producción hasta aprobación expresa.

---

# 27. Nota pedagógica: por qué este documento es un SDD

Este documento no se limita a indicar **qué** debe hacer CrossEd. También especifica **cómo está diseñado** para hacerlo: arquitectura, módulos, modelo de datos, reglas algorítmicas, contratos API, seguridad, tiempo real, manejo de errores y decisiones de interfaz.

Ejemplo:

- Requisito: `El alumno debe poder participar desde un celular.`
- Diseño: `Frontend React/Next.js responsive; backend Spring Boot; estado en Redis; WebSocket/STOMP para eventos.`

Por eso puede utilizarse como ejemplo académico de **Software Design Description (SDD)** y como entrada estructurada para desarrollo asistido por IA.

---

## Regla final para la implementación

**El documento define el comportamiento. El código debe adaptarse al SDD, no el SDD al código.** Si durante la construcción una decisión técnica obliga a alterar una regla funcional, detener esa modificación y solicitar aprobación.


---

# 28. Contenerización con Docker

## 28.1 Objetivo

CrossEd debe quedar preparado para ejecutarse de forma reproducible mediante contenedores. La contenerización forma parte del diseño del MVP y debe permitir dos usos:

1. levantar el entorno completo de desarrollo local con un solo comando;
2. preparar el proyecto para un despliegue posterior sin modificar la arquitectura de la aplicación.

El uso de Docker no cambia las reglas funcionales del sistema. Su función es empaquetar y aislar los componentes de infraestructura.

## 28.2 Componentes contenerizados

El entorno local debe contemplar tres servicios:

```text
crossed
├── frontend   -> Next.js
├── backend    -> Spring Boot + Java 21
└── redis      -> estado temporal
```

Cada componente se ejecuta de manera aislada y se comunica mediante una red de Docker Compose.

## 28.3 Estructura de infraestructura esperada

```text
CrossEd/
├── backend/
│   ├── Dockerfile
│   ├── .dockerignore
│   └── ...
├── frontend/
│   ├── Dockerfile
│   ├── .dockerignore
│   └── ...
├── docker-compose.yml
├── .env.example
└── README.md
```

Work puede ajustar nombres menores si el framework generado lo requiere, pero debe conservar esta separación conceptual.

## 28.4 Dockerfile del backend

El backend debe usar una construcción multi-stage para evitar incluir Maven y fuentes innecesarias en la imagen final.

Requisitos:

- compilación con Maven;
- runtime Java 21;
- imagen final liviana;
- puerto interno 8080;
- variables de entorno para Redis, IA, correo, CORS y perfil de Spring;
- health check disponible mediante un endpoint de salud;
- no incluir secretos dentro de la imagen.

Ejemplo de referencia incluido con este SDD: `docker/backend/Dockerfile`.

## 28.5 Dockerfile del frontend

El frontend debe usar build multi-stage para Next.js.

Requisitos:

- Node LTS compatible con la versión de Next.js elegida;
- instalación reproducible de dependencias;
- build de producción;
- imagen final sin dependencias de desarrollo innecesarias;
- puerto interno 3000;
- URL del backend configurable mediante variables de entorno;
- URL de WebSocket configurable para desarrollo y producción.

Ejemplo de referencia incluido con este SDD: `docker/frontend/Dockerfile`.

## 28.6 Docker Compose local

El archivo `docker-compose.yml` debe levantar:

- `redis`;
- `backend`;
- `frontend`.

Dependencias conceptuales:

```text
frontend -> backend -> redis
```

El backend no debe usar `localhost` para acceder a Redis dentro de Compose. Debe utilizar el nombre del servicio, por ejemplo `redis`.

Puertos locales sugeridos:

- Frontend: `3000:3000`
- Backend: `8080:8080`
- Redis: `6379:6379`

Redis no debe publicarse hacia Internet en producción.

## 28.7 Variables de entorno

Debe existir un `.env.example` sin secretos reales.

Variables mínimas previstas:

```text
SPRING_PROFILES_ACTIVE
REDIS_HOST
REDIS_PORT
OPENAI_API_KEY
GEMINI_API_KEY
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
MAIL_FROM
CORS_ALLOWED_ORIGINS
NEXT_PUBLIC_API_URL
NEXT_PUBLIC_WS_URL
```

Si durante la implementación Spring AI o el proveedor de correo requieren nombres diferentes, documentarlos en el README sin almacenar valores reales.

## 28.8 Health checks

El backend debe exponer un endpoint de salud apropiado, preferentemente usando Spring Boot Actuator.

Objetivo:

```text
GET /actuator/health
```

Debe permitir a Docker y al hosting saber si el backend está listo.

Docker Compose puede usar este endpoint como `healthcheck` antes de considerar disponible el servicio.

Redis debe tener su propio health check con `redis-cli ping`.

## 28.9 Perfiles de Spring

Se recomienda utilizar al menos:

- `local`: desarrollo sin Docker si fuera necesario;
- `docker`: ejecución con Docker Compose;
- `prod`: despliegue público.

La configuración sensible se obtiene siempre desde variables de entorno.

## 28.10 CORS y WebSocket

No fijar dominios de producción directamente en código.

Debe existir una variable como:

```text
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

En producción se reemplaza por el dominio real del frontend.

WebSocket debe funcionar con:

- `ws://` en desarrollo local;
- `wss://` en producción HTTPS.

El frontend debe construir o recibir su URL mediante configuración de entorno.

## 28.11 Persistencia de Redis en desarrollo

Para desarrollo puede utilizarse un volumen Docker nombrado para conservar estado durante reinicios accidentales del contenedor Redis.

Ejemplo conceptual:

```text
crossed_redis_data
```

Los datos siguen siendo temporales y deben respetar el TTL definido por la aplicación.

## 28.12 Comandos esperados

El proyecto debe poder levantarse localmente con:

```bash
docker compose up --build
```

Y detenerse con:

```bash
docker compose down
```

Para eliminar también el volumen local de Redis cuando se necesite una limpieza total:

```bash
docker compose down -v
```

## 28.13 Criterios de aceptación de Docker

La contenerización se considera correcta cuando:

1. `docker compose up --build` construye los servicios sin intervención manual adicional;
2. el frontend abre en `http://localhost:3000`;
3. el backend responde en `http://localhost:8080`;
4. el backend puede acceder a Redis;
5. el frontend puede consumir REST y WebSocket del backend;
6. los secretos no están incluidos en imágenes ni repositorio;
7. los contenedores pueden recrearse sin modificar código;
8. el README explica cómo iniciar, probar y detener el entorno.

---

# 29. Estrategia de despliegue del MVP

## 29.1 Objetivo

La primera versión debe quedar preparada para despliegue, pero Work **no debe publicarla** hasta recibir aprobación explícita.

Arquitectura objetivo de bajo costo:

```text
Usuario
   |
   v
Frontend Next.js
   |
 HTTPS / WSS
   v
Backend Spring Boot
   |
   v
Redis administrado
```

## 29.2 Separación de responsabilidades

El diseño debe permitir que cada componente pueda desplegarse independientemente:

- Frontend: servicio apto para Next.js, por ejemplo Vercel.
- Backend: servicio que acepte contenedores Docker, por ejemplo Render u otro equivalente.
- Redis: servicio Redis administrado, por ejemplo Upstash u otro equivalente.

Los nombres de proveedores son el objetivo inicial del MVP, no una dependencia de dominio. La aplicación debe mantenerse portable.

## 29.3 Reglas de despliegue

- HTTPS obligatorio en producción.
- WebSocket mediante WSS.
- Redis no expuesto públicamente.
- API keys y credenciales configuradas mediante secretos del proveedor.
- CORS limitado al dominio real del frontend.
- Logs sin tokens ni credenciales.
- Health check activo en backend.
- Variables de entorno documentadas.
- No desplegar con archivos `.env` que contengan secretos reales dentro del repositorio.

## 29.4 Costos externos

Aunque la infraestructura pueda utilizar niveles gratuitos para demostración, las llamadas de IA y/o correo pueden generar costo dependiendo del proveedor y del uso.

CrossEd limita el consumo de IA al proceso de generación del crucigrama. Durante la partida multijugador no deben realizarse llamadas de IA.

## 29.5 Proceso de aprobación

Work debe dejar documentado el procedimiento de despliegue, pero detenerse antes de ejecutarlo.

Flujo esperado:

```text
Construir localmente
      ↓
Pruebas
      ↓
Revisión del propietario
      ↓
Aprobación explícita
      ↓
Despliegue
```

---

# 30. Entregables Docker obligatorios de Work

Además de los entregables anteriores, Work debe generar:

1. `backend/Dockerfile`.
2. `backend/.dockerignore`.
3. `frontend/Dockerfile`.
4. `frontend/.dockerignore`.
5. `docker-compose.yml`.
6. `.env.example` sin secretos.
7. configuración de health check del backend.
8. configuración de Redis por variables de entorno.
9. configuración de CORS por variables de entorno.
10. configuración de URL REST y WebSocket en frontend por entorno.
11. README con ejecución local mediante Docker.
12. sección de README con procedimiento de despliegue futuro.
13. comprobación de que no existen secretos reales en los archivos entregados.
14. no ejecutar el despliegue público sin autorización.

---

## Regla final de infraestructura

**Docker debe facilitar la reproducción y el despliegue del sistema, pero no debe introducir reglas de negocio ni acoplar CrossEd a un proveedor específico.**
