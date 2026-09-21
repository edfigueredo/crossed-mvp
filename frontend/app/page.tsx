"use client";
import { useEffect, useRef, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { api, normalizar } from "../servicios/api";
import { conectar } from "../servicios/websocket";
import type { Concepto, Grilla, Sesion, Vista } from "../tipos";
import Conejo from "../componentes/Conejo";
import Tablero from "../componentes/Tablero";
import Tabla, { tiempo } from "../componentes/Tabla";
const roles = [
  "Creador",
  "Arquitecto",
  "Domador de Ideas",
  "Cebador de Mate",
  "Programador",
  "Tester Oficial",
  "Cazador de Bugs",
  "Maestro del Ctrl+Z",
  "Guardián del Deploy",
];
const ejemplo: Concepto[] = [
  {
    palabra: "CONTENEDOR",
    definicion: "Unidad aislada que ejecuta una aplicación y sus dependencias.",
  },
  {
    palabra: "IMAGEN",
    definicion: "Plantilla inmutable utilizada para crear contenedores.",
  },
  {
    palabra: "DOCKER",
    definicion: "Plataforma que permite construir y ejecutar contenedores.",
  },
  {
    palabra: "VOLUMEN",
    definicion:
      "Mecanismo para conservar datos fuera de la capa escribible del contenedor.",
  },
  {
    palabra: "PUERTO",
    definicion:
      "Número que identifica un punto de comunicación de un servicio.",
  },
  { palabra: "RED", definicion: "Permite la comunicación entre contenedores." },
  {
    palabra: "SERVICIO",
    definicion: "Componente de una aplicación definido en Compose.",
  },
  {
    palabra: "REGISTRO",
    definicion: "Repositorio donde se almacenan y distribuyen imágenes.",
  },
  {
    palabra: "CAPA",
    definicion: "Cambio del sistema de archivos que forma parte de una imagen.",
  },
  {
    palabra: "COMPOSE",
    definicion: "Herramienta para definir aplicaciones de varios contenedores.",
  },
  {
    palabra: "VIRTUALIZACION",
    definicion:
      "Técnica que abstrae recursos físicos para crear entornos lógicos.",
  },
  {
    palabra: "DEPENDENCIA",
    definicion: "Componente requerido por una aplicación para funcionar.",
  },
  { palabra: "PROCESO", definicion: "Programa en ejecución." },
  {
    palabra: "SISTEMA",
    definicion:
      "Conjunto de componentes que interactúan para cumplir una función.",
  },
  {
    palabra: "TERMINAL",
    definicion: "Interfaz de texto donde se introducen comandos.",
  },
];
export default function Inicio() {
  const [pantalla, setPantalla] = useState("inicio"),
    [sesion, setSesion] = useState<Sesion | null>(null),
    [vista, setVista] = useState<Vista | null>(null),
    [codigo, setCodigo] = useState(""),
    [error, setError] = useState(""),
    [ocupado, setOcupado] = useState(false),
    [rol, setRol] = useState(0),
    [aviso, setAviso] = useState(""),
    [cuenta, setCuenta] = useState<number | null>(null);
  const [datos, setDatos] = useState({
    nombre: "",
    materia: "",
    nombreProfesor: "",
    correoProfesor: "",
    idioma: "es",
    duracionMinutos: 20,
    mostrarResultados: true,
    intentosPorPalabra: 0,
    cantidadPalabrasSolicitada: 10,
  });
  const [fuente, setFuente] = useState(""),
    [url, setUrl] = useState(""),
    [conceptos, setConceptos] = useState<Concepto[]>([]),
    [previa, setPrevia] = useState<Grilla | null>(null),
    [activa, setActiva] = useState(1),
    [respuesta, setRespuesta] = useState(""),
    [respuestas, setRespuestas] = useState<Record<number, string>>({}),
    [feedback, setFeedback] = useState(""),
    [nombre, setNombre] = useState(""),
    [correo, setCorreo] = useState("");
  const anterior = useRef(""),
    contador = useRef<ReturnType<typeof setInterval> | null>(null),
    inputRespuesta = useRef<HTMLInputElement>(null);
  useEffect(() => {
    const c = new URLSearchParams(location.search).get("codigo");
    if (c) {
      setCodigo(c);
      setPantalla("ingreso");
    }
    const guardada = sessionStorage.getItem("crossed-profesor");
    if (guardada && !c) {
      setSesion(JSON.parse(guardada));
      setPantalla("panel");
    }
    const t = setInterval(() => setRol((i) => (i + 1) % roles.length), 5000);
    return () => clearInterval(t);
  }, []);
  const ejecutar = async (fn: () => Promise<void>) => {
    setError("");
    setOcupado(true);
    try {
      await fn();
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "No se pudo completar la operación.",
      );
    } finally {
      setOcupado(false);
    }
  };
  useEffect(() => {
    if (!sesion) return;
    let viva = true;
    const actualizar = async () => {
      try {
        const v = await api<Vista>(
          `/api/partidas/${sesion.idPartida}` +
            (sesion.profesor ? "" : `/jugadores/${sesion.idJugador}/heartbeat`),
          sesion.profesor ? "GET" : "POST",
          sesion.profesor ? undefined : { pantallaActiva: !document.hidden },
          sesion.token,
        );
        if (!viva) return;
        setVista(v);
        if (
          !sesion.profesor &&
          v.estado === "EN_CURSO" &&
          anterior.current === "ESPERANDO"
        ) {
          setCuenta(3);
          if (contador.current) clearInterval(contador.current);
          contador.current = setInterval(
            () =>
              setCuenta((n) => {
                if (n === null || n <= 0) {
                  if (contador.current) clearInterval(contador.current);
                  return null;
                }
                return n - 1;
              }),
            1000,
          );
        }
        anterior.current = v.estado;
      } catch (e) {
        if (viva)
          setError(e instanceof Error ? e.message : "Conexión interrumpida.");
      }
    };
    void actualizar();
    const timer = setInterval(actualizar, 5000);
    const desconectar = conectar(sesion, (tipo) => {
      if (tipo === "QUEDAN_TRES_MINUTOS")
        setAviso("¡Quedan 3 minutos! El portero pregunta si tenés llave 🔑");
      if (tipo === "QUEDA_UN_MINUTO")
        setAviso("¡Queda 1 minuto! Dicen que apagues la luz cuando salgas 😅");
      void actualizar();
    });
    document.addEventListener("visibilitychange", actualizar);
    return () => {
      viva = false;
      clearInterval(timer);
      desconectar();
      document.removeEventListener("visibilitychange", actualizar);
    };
  }, [sesion]);
  useEffect(() => {
    if (sesion?.profesor || vista?.estado !== "EN_CURSO") return;
    const t = setInterval(() => {
      const fase = vista.tension;
      const mensajes =
        fase < 3
          ? ["Cada vez quedan menos…", "Las ideas se cruzan. ¡Vos podés!"]
          : fase < 5
            ? ["En cualquier momento tocan el himno.", "No hay más sol 🌙"]
            : [
                "El portero pregunta si tenés llave 🔑",
                "Hasta el conejo está mirando la puerta…",
              ];
      setAviso(mensajes[Math.floor(Math.random() * mensajes.length)]);
    }, 30000);
    return () => clearInterval(t);
  }, [vista?.tension, vista?.estado, sesion?.profesor]);
  const cambiar = (indice: number, campo: keyof Concepto, valor: string) => {
    setConceptos((cs) =>
      cs.map((c, i) => (i === indice ? { ...c, [campo]: valor } : c)),
    );
    if (campo === "palabra") setPrevia(null);
  };
  const generar = async () => {
    const g = await api<Grilla>("/api/crucigramas/generar", "POST", {
      conceptos,
      cantidad: datos.cantidadPalabrasSolicitada,
    });
    setPrevia(g);
    setActiva(g.palabras[0].idPalabra);
  };
  const refrescar = async () => {
    if (sesion)
      setVista(
        await api<Vista>(
          `/api/partidas/${sesion.idPartida}`,
          "GET",
          undefined,
          sesion.token,
        ),
      );
  };
  const seleccionar = (id: number) => {
    setActiva(id);
    setRespuesta("");
    setFeedback("");
    inputRespuesta.current?.focus();
  };
  const palabra = vista?.crucigrama?.palabras.find(
    (p) => p.idPalabra === activa,
  );
  const enlace =
    typeof window !== "undefined" && vista
      ? `${location.origin}/?codigo=${vista.codigoVisible}`
      : "";
  return (
    <>
      <header>
        <a className="marca" href="/" aria-label="CrossEd inicio">
          <span className="logo">▦</span>Cross<span>Ed</span>
        </a>
        <span className="cabecera-texto">APRENDER SE CRUZA CON JUGAR</span>
        <span className="etiqueta">EDICIÓN AULA</span>
      </header>
      <main>
        {error && (
          <div role="alert" className="error">
            {error}
            <button className="texto" onClick={() => setError("")}>
              Cerrar
            </button>
          </div>
        )}
        {ocupado && (
          <div role="status" className="procesando">
            Procesando…
          </div>
        )}
        {!sesion && pantalla === "inicio" && (
          <section className="hero">
            <div>
              <span className="kicker">CONOCIMIENTO EN JUEGO</span>
              <h1>
                Una clase.
                <br />
                Muchas ideas.
                <br />
                <em>Un gran cruce.</em>
              </h1>
              <p>
                Convertí tu material de estudio en un crucigrama. Compartí la
                sala y dejá que empiece el desafío.
              </p>
              <div className="acciones">
                <button onClick={() => setPantalla("crear")}>
                  Crear crucigrama <span>↗</span>
                </button>
                <button
                  className="secundario"
                  onClick={() => setPantalla("ingreso")}
                >
                  Soy alumno →
                </button>
              </div>
              <div className="detalles">
                <span>01 · Prepará</span>
                <span>02 · Compartí</span>
                <span>03 · Jugá</span>
              </div>
            </div>
            <div className="hero-juego">
              <div className="ventana-titulo">
                <span>crossed / sala de ideas</span>
                <span>● ● ●</span>
              </div>
              <div className="letras-demo">
                {"APRENDER".split("").map((l, i) => (
                  <span key={i}>{l}</span>
                ))}
              </div>
              <Conejo />
              <div className="pie-demo">
                TU PRÓXIMA CLASE, EN MODO JUEGO <span>✦</span>
              </div>
            </div>
          </section>
        )}
        {!sesion && pantalla === "crear" && (
          <>
            <div className="titulo-seccion">
              <div>
                <span className="kicker">ESPACIO DEL PROFESOR</span>
                <h1>Prepará el desafío</h1>
                <p>
                  Configurá tu clase, cargá una fuente y revisá los conceptos.
                </p>
              </div>
              <button
                className="secundario"
                onClick={() => setPantalla("inicio")}
              >
                Volver
              </button>
            </div>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                void ejecutar(async () => {
                  const s = await api<Sesion>("/api/partidas", "POST", {
                    ...datos,
                    conceptos,
                  });
                  const nueva = { ...s, profesor: true };
                  sessionStorage.setItem(
                    "crossed-profesor",
                    JSON.stringify(nueva),
                  );
                  setSesion(nueva);
                  setPantalla("panel");
                });
              }}
            >
              <section className="tarjeta">
                <h2>
                  <b>01</b> Tu partida
                </h2>
                <div className="form-grid">
                  {(
                    [
                      "nombre",
                      "materia",
                      "nombreProfesor",
                      "correoProfesor",
                    ] as const
                  ).map((k, i) => (
                    <label key={k}>
                      {
                        [
                          "Nombre de la partida",
                          "Materia",
                          "Nombre del profesor",
                          "Correo del profesor",
                        ][i]
                      }
                      <input
                        required
                        type={k === "correoProfesor" ? "email" : "text"}
                        maxLength={k === "correoProfesor" ? 254 : 100}
                        value={datos[k]}
                        onChange={(e) =>
                          setDatos({ ...datos, [k]: e.target.value })
                        }
                      />
                    </label>
                  ))}
                  <label>
                    Idioma de los conceptos
                    <select
                      value={datos.idioma}
                      onChange={(e) =>
                        setDatos({ ...datos, idioma: e.target.value })
                      }
                    >
                      <option value="es">Español</option>
                      <option value="en">English</option>
                    </select>
                  </label>
                  <label>
                    Duración (minutos)
                    <input
                      type="number"
                      min="1"
                      max="180"
                      required
                      value={datos.duracionMinutos}
                      onChange={(e) =>
                        setDatos({ ...datos, duracionMinutos: +e.target.value })
                      }
                    />
                  </label>
                  <label>
                    Cantidad de palabras
                    <select
                      value={datos.cantidadPalabrasSolicitada}
                      onChange={(e) =>
                        setDatos({
                          ...datos,
                          cantidadPalabrasSolicitada: +e.target.value,
                        })
                      }
                    >
                      {[5, 8, 10, 12, 15].map((n) => (
                        <option key={n}>{n}</option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Intentos por palabra
                    <select
                      value={datos.intentosPorPalabra}
                      onChange={(e) =>
                        setDatos({
                          ...datos,
                          intentosPorPalabra: +e.target.value,
                        })
                      }
                    >
                      {[0, 1, 2, 3, 5].map((n) => (
                        <option key={n} value={n}>
                          {n || "Infinitos"}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>
                <label className="check">
                  <input
                    type="checkbox"
                    checked={datos.mostrarResultados}
                    onChange={(e) =>
                      setDatos({
                        ...datos,
                        mostrarResultados: e.target.checked,
                      })
                    }
                  />{" "}
                  Mostrar ranking al finalizar (si no, cada alumno recibe su
                  resultado por correo)
                </label>
              </section>
              <section className="tarjeta">
                <h2>
                  <b>02</b> El material de estudio
                </h2>
                <label>
                  Pegá texto del material
                  <textarea
                    rows={6}
                    maxLength={60000}
                    value={fuente}
                    onChange={(e) => setFuente(e.target.value)}
                    placeholder="Definiciones, apuntes o un capítulo de tu clase…"
                  />
                </label>
                <div className="form-grid">
                  <label>
                    Archivo · PDF, Word, PowerPoint, Excel o TXT
                    <input
                      type="file"
                      accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt"
                      onChange={(e) => {
                        const archivo = e.target.files?.[0];
                        if (archivo)
                          void ejecutar(async () => {
                            const f = new FormData();
                            f.append("archivo", archivo);
                            const r = await api<{ texto: string }>(
                              "/api/contenido/archivo",
                              "POST",
                              f,
                            );
                            setFuente(r.texto);
                          });
                      }}
                    />
                  </label>
                  <label>
                    Enlace público
                    <div className="en-linea">
                      <input
                        type="url"
                        value={url}
                        onChange={(e) => setUrl(e.target.value)}
                        placeholder="https://…"
                      />
                      <button
                        type="button"
                        className="secundario"
                        disabled={ocupado || !url}
                        onClick={() =>
                          void ejecutar(async () => {
                            const r = await api<{ texto: string }>(
                              "/api/contenido/enlace",
                              "POST",
                              { url },
                            );
                            setFuente(r.texto);
                          })
                        }
                      >
                        Leer
                      </button>
                    </div>
                  </label>
                </div>
                <p className="ayuda">
                  Los enlaces deben ofrecer texto sin iniciar sesión. Si la
                  plataforma no lo permite, exportá el archivo. Revisá las
                  pistas y su evidencia antes de compartir.
                </p>
                <div className="acciones">
                  <button
                    type="button"
                    disabled={ocupado || fuente.length < 80}
                    onClick={() =>
                      void ejecutar(async () => {
                        setConceptos(
                          await api<Concepto[]>(
                            "/api/crucigramas/conceptos",
                            "POST",
                            {
                              texto: fuente,
                              cantidad: datos.cantidadPalabrasSolicitada,
                              idioma: datos.idioma,
                            },
                          ),
                        );
                        setPrevia(null);
                      })
                    }
                  >
                    Proponer conceptos con IA ✦
                  </button>
                  <button
                    type="button"
                    className="secundario"
                    onClick={() => {
                      setConceptos(ejemplo);
                      setPrevia(null);
                    }}
                  >
                    Probar ejemplo Docker
                  </button>
                </div>
              </section>
              <section className="tarjeta">
                <h2>
                  <b>03</b> Revisá y organizá
                </h2>
                <Editor
                  conceptos={conceptos}
                  cambiar={cambiar}
                  quitar={(i) => {
                    setConceptos((cs) => cs.filter((_, n) => n !== i));
                    setPrevia(null);
                  }}
                />
                <div className="acciones">
                  <button
                    type="button"
                    className="secundario"
                    disabled={conceptos.length >= 20}
                    onClick={() =>
                      setConceptos([
                        ...conceptos,
                        { palabra: "", definicion: "" },
                      ])
                    }
                  >
                    + Concepto manual
                  </button>
                  <button
                    type="button"
                    disabled={ocupado || conceptos.length < 5}
                    onClick={() => void ejecutar(generar)}
                  >
                    Generar tablero
                  </button>
                </div>
                {previa && (
                  <>
                    <p className="exito">
                      Tablero validado: {previa.palabras.length} de{" "}
                      {datos.cantidadPalabrasSolicitada} palabras solicitadas.
                      {previa.palabras.length <
                        datos.cantidadPalabrasSolicitada &&
                        " Se redujo la cantidad para mantener cruces válidos."}
                    </p>
                    <Tablero
                      grilla={previa}
                      activa={activa}
                      elegir={setActiva}
                      previa
                    />
                  </>
                )}
              </section>
              <button className="boton-grande" disabled={ocupado || !previa}>
                Crear sala y obtener QR →
              </button>
            </form>
          </>
        )}
        {!sesion && pantalla === "ingreso" && (
          <section className="ingreso tarjeta">
            <span className="kicker">ENTRÁ A LA PARTIDA</span>
            <h1>Tu lugar está acá.</h1>
            <p>Pedile el código a tu profesor.</p>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                void ejecutar(async () => {
                  const v = await api<Vista>(
                    `/api/partidas/codigo/${encodeURIComponent(codigo)}`,
                  );
                  const j = await api<{ idJugador: string; token: string }>(
                    `/api/partidas/${v.idPartida}/jugadores`,
                    "POST",
                    { nombre, correo },
                  );
                  anterior.current = v.estado;
                  setVista(v);
                  setSesion({
                    ...j,
                    idPartida: v.idPartida,
                    codigoVisible: v.codigoVisible,
                    profesor: false,
                  });
                });
              }}
            >
              <label>
                Código de sala
                <input
                  required
                  maxLength={10}
                  value={codigo}
                  onChange={(e) => setCodigo(e.target.value.toUpperCase())}
                  className="codigo-input"
                />
              </label>
              <label>
                Tu nombre
                <input
                  required
                  maxLength={100}
                  value={nombre}
                  onChange={(e) => setNombre(e.target.value)}
                />
              </label>
              <label>
                Tu correo
                <input
                  required
                  type="email"
                  maxLength={254}
                  value={correo}
                  onChange={(e) => setCorreo(e.target.value)}
                />
              </label>
              <p className="ayuda">
                Tu registro es temporal. Si salís o perdés la conexión durante
                más de 15 segundos, volverás a empezar desde cero.
              </p>
              <button disabled={ocupado}>Entrar al desafío →</button>
            </form>
          </section>
        )}
        {sesion && vista && sesion.profesor && (
          <>
            <div className="titulo-seccion">
              <div>
                <span className="kicker">
                  PANEL DEL PROFESOR · {vista.estado.replace("_", " ")}
                </span>
                <h1>{vista.nombre}</h1>
                <p>{vista.materia}</p>
              </div>
              <div className="codigo-panel">
                CÓDIGO<strong>{vista.codigoVisible}</strong>
              </div>
            </div>
            <div className="metricas">
              <div>
                <span>Participantes</span>
                <strong>
                  {vista.cantidadJugadores}
                  <small> / 60</small>
                </strong>
              </div>
              <div>
                <span>Completaron</span>
                <strong>
                  {vista.jugadores?.filter((j) => j.finalizo).length || 0}
                </strong>
              </div>
              <div>
                <span>Tiempo restante</span>
                <strong>
                  {vista.estado === "EN_CURSO"
                    ? tiempo((vista.segundosRestantes ?? 0) * 1000)
                    : "—"}
                </strong>
              </div>
            </div>
            {vista.estado === "ESPERANDO" && (
              <section className="tarjeta compartir">
                <QRCodeSVG value={enlace} size={160} />
                <div>
                  <h2>La sala está lista.</h2>
                  <p>Compartí el QR o el enlace con tus alumnos.</p>
                  <a href={enlace} target="_blank" rel="noreferrer">
                    {enlace}
                  </a>
                  <div className="acciones">
                    <button
                      disabled={ocupado}
                      onClick={() =>
                        void ejecutar(async () => {
                          await api(
                            `/api/partidas/${sesion.idPartida}/iniciar`,
                            "POST",
                            undefined,
                            sesion.token,
                          );
                          await refrescar();
                        })
                      }
                    >
                      Iniciar partida ▶
                    </button>
                    <button
                      className="secundario"
                      onClick={() => {
                        setConceptos(
                          vista.crucigrama!.palabras.map((p) => ({
                            palabra: p.palabra!,
                            definicion: p.definicion,
                          })),
                        );
                        setPantalla("editar");
                      }}
                    >
                      Editar crucigrama
                    </button>
                  </div>
                </div>
              </section>
            )}
            {pantalla === "editar" && vista.estado === "ESPERANDO" && (
              <section className="tarjeta">
                <h2>Editar vista previa</h2>
                <Editor
                  conceptos={conceptos}
                  cambiar={cambiar}
                  quitar={(i) =>
                    setConceptos((cs) => cs.filter((_, n) => i !== n))
                  }
                />
                <button
                  disabled={ocupado || conceptos.length < 5}
                  onClick={() =>
                    void ejecutar(async () => {
                      await api(
                        `/api/partidas/${sesion.idPartida}/crucigrama`,
                        "PUT",
                        { conceptos },
                        sesion.token,
                      );
                      await refrescar();
                      setPantalla("panel");
                    })
                  }
                >
                  Guardar y validar
                </button>
              </section>
            )}
            {vista.estado === "ESPERANDO" && vista.crucigrama && (
              <details className="tarjeta">
                <summary>Ver tablero y respuestas · solo profesor</summary>
                <Tablero
                  grilla={vista.crucigrama}
                  activa={activa}
                  elegir={setActiva}
                  previa
                />
              </details>
            )}
            <section className="tarjeta">
              <h2>
                {vista.estado === "FINALIZADA"
                  ? "Resultado final"
                  : "Participantes en vivo"}
              </h2>
              <Tabla filas={vista.ranking ?? vista.jugadores ?? []} />
              <p className="ayuda">
                Una fila roja indica que la pantalla no está activa. Es una
                señal visual, no una acusación.
              </p>
            </section>
            {vista.estado === "EN_CURSO" && (
              <button
                className="peligro"
                disabled={ocupado}
                onClick={() => {
                  if (confirm("¿Seguro que querés finalizar la partida?"))
                    void ejecutar(async () => {
                      await api(
                        `/api/partidas/${sesion.idPartida}/finalizar`,
                        "POST",
                        undefined,
                        sesion.token,
                      );
                      await refrescar();
                    });
                }}
              >
                Finalizar partida
              </button>
            )}
            {vista.estado === "FINALIZADA" && (
              <section className="tarjeta">
                <h2>¡Crucigrama finalizado!</h2>
                <p>
                  {vista.correos?.every((c) => c === "ENVIADO")
                    ? "Los resultados fueron procesados y los correos enviados correctamente."
                    : vista.correos?.some((c) => c === "ERROR")
                      ? "Los resultados están guardados. No se pudieron enviar todos los correos; revisá la configuración y reintentá."
                      : "Resultados procesados. Envío de correos pendiente."}
                </p>
                <div className="acciones">
                  <button
                    onClick={() => {
                      if (
                        confirm(
                          "Se eliminará la sesión y sus resultados. ¿Crear un nuevo crucigrama?",
                        )
                      )
                        void ejecutar(async () => {
                          await api(
                            `/api/partidas/${sesion.idPartida}`,
                            "DELETE",
                            undefined,
                            sesion.token,
                          );
                          sessionStorage.removeItem("crossed-profesor");
                          setSesion(null);
                          setVista(null);
                          setPrevia(null);
                          setConceptos([]);
                          setPantalla("crear");
                        });
                    }}
                  >
                    NUEVO CRUCIGRAMA
                  </button>
                  <button
                    className="secundario"
                    onClick={() =>
                      void ejecutar(async () => {
                        await api(
                          `/api/partidas/${sesion.idPartida}/correos/reintentar`,
                          "POST",
                          undefined,
                          sesion.token,
                        );
                        await refrescar();
                      })
                    }
                  >
                    Reintentar correos
                  </button>
                </div>
              </section>
            )}
          </>
        )}
        {sesion && vista && !sesion.profesor && (
          <>
            <div className="titulo-seccion">
              <div>
                <span className="kicker">
                  {vista.materia} · SALA {vista.codigoVisible}
                </span>
                <h1>{vista.nombre}</h1>
              </div>
              <span className="etiqueta">{nombre}</span>
            </div>
            {vista.estado === "ESPERANDO" && (
              <section className="espera tarjeta">
                <Conejo />
                <h2>Ya somos {vista.cantidadJugadores} participantes</h2>
                <p>Esperando que el profesor inicie la partida…</p>
                <span className="puntos">● ● ●</span>
              </section>
            )}
            {vista.estado === "EN_CURSO" && (
              <>
                <Conejo estado={vista.tension} />
                <div className="aviso" role="status">
                  {aviso || "Cada pista es una oportunidad. ¡A cruzar ideas!"}
                </div>
                {vista.finalizo ? (
                  <section className="tarjeta espera">
                    <h2>¡Lo completaste! ✦</h2>
                    <p>
                      Esperá aquí hasta que termine la partida. Mantené la
                      sesión abierta para conservar tu resultado.
                    </p>
                  </section>
                ) : (
                  vista.crucigrama && (
                    <div className="juego">
                      <section className="tarjeta">
                        <Tablero
                          grilla={vista.crucigrama}
                          activa={activa}
                          elegir={seleccionar}
                          respuestas={respuestas}
                          progreso={vista.progreso}
                        />
                      </section>
                      <section className="tarjeta pistas">
                        <span className="kicker">PISTA ACTIVA · {activa}</span>
                        <h2>
                          {palabra?.definicion ||
                            "Elegí una palabra del tablero"}
                        </h2>
                        <p>
                          {palabra?.cantidadLetras} letras ·{" "}
                          {palabra?.direccion === "HORIZONTAL"
                            ? "Horizontal"
                            : "Vertical"}
                        </p>
                        <form
                          onSubmit={(e) => {
                            e.preventDefault();
                            void ejecutar(async () => {
                              const r = await api<{
                                correcta: boolean;
                                bloqueada: boolean;
                                intentosRestantes: number;
                              }>(
                                `/api/partidas/${sesion.idPartida}/jugadores/${sesion.idJugador}/respuestas`,
                                "POST",
                                { idPalabra: activa, respuesta },
                                sesion.token,
                              );
                              if (r.correcta)
                                setRespuestas((rs) => ({
                                  ...rs,
                                  [activa]: normalizar(respuesta),
                                }));
                              setFeedback(
                                r.correcta
                                  ? "¡Correcto! Palabra completada."
                                  : r.bloqueada
                                    ? "Se agotaron los intentos para esta palabra."
                                    : `Todavía no. Probá de nuevo.${r.intentosRestantes >= 0 ? " Intentos restantes: " + r.intentosRestantes : ""}`,
                              );
                              setVista(
                                await api<Vista>(
                                  `/api/partidas/${sesion.idPartida}/jugadores/${sesion.idJugador}/heartbeat`,
                                  "POST",
                                  { pantallaActiva: !document.hidden },
                                  sesion.token,
                                ),
                              );
                            });
                          }}
                        >
                          <label>
                            Tu respuesta
                            <input
                              ref={inputRespuesta}
                              required
                              maxLength={100}
                              autoComplete="off"
                              value={respuesta}
                              onChange={(e) => setRespuesta(e.target.value)}
                              disabled={vista.progreso?.[activa]?.bloqueada}
                            />
                          </label>
                          <button
                            disabled={
                              ocupado ||
                              !palabra ||
                              vista.progreso?.[activa]?.bloqueada
                            }
                          >
                            Comprobar →
                          </button>
                          <p
                            role="status"
                            className={
                              feedback.startsWith("¡") ? "exito" : "incorrecta"
                            }
                          >
                            {feedback}
                          </p>
                        </form>
                        <div className="lista-pistas">
                          {vista.crucigrama.palabras.map((p) => (
                            <button
                              key={p.idPalabra}
                              className={
                                "pista " +
                                (activa === p.idPalabra ? "seleccionada" : "")
                              }
                              onClick={() => seleccionar(p.idPalabra)}
                            >
                              <b>{p.idPalabra}</b> {p.definicion}{" "}
                              {vista.progreso?.[p.idPalabra]?.correcta
                                ? "✓"
                                : vista.progreso?.[p.idPalabra]?.bloqueada
                                  ? "🔒"
                                  : ""}
                            </button>
                          ))}
                        </div>
                      </section>
                    </div>
                  )
                )}
              </>
            )}
            {vista.estado === "FINALIZADA" && (
              <section className="tarjeta">
                <Conejo estado={7} />
                <h2>¡Desafío finalizado!</h2>
                {vista.mostrarResultados ? (
                  <Tabla filas={vista.ranking ?? []} />
                ) : (
                  <p>
                    El ranking no es público. El sistema intentará enviar tu
                    resultado a tu correo.
                  </p>
                )}
              </section>
            )}
            <button
              className="texto"
              onClick={() => {
                if (confirm("Al salir se elimina tu progreso activo. ¿Salir?"))
                  void ejecutar(async () => {
                    await api(
                      `/api/partidas/${sesion.idPartida}/jugadores/${sesion.idJugador}`,
                      "DELETE",
                      undefined,
                      sesion.token,
                    );
                    setSesion(null);
                    setVista(null);
                    setRespuestas({});
                    setPantalla("ingreso");
                  });
              }}
            >
              Salir de la sala
            </button>
          </>
        )}
        {cuenta !== null && (
          <div className="cuenta" aria-live="assertive">
            {cuenta === 0 ? "¡YAAAA!" : cuenta}
          </div>
        )}
      </main>
      <footer>
        <span>CrossEd · El conocimiento conecta.</span>
        <span>
          {roles[rol]} <b>Eduardo Figueredo</b>
        </span>
      </footer>
    </>
  );
}
function Editor({
  conceptos,
  cambiar,
  quitar,
}: {
  conceptos: Concepto[];
  cambiar: (i: number, c: keyof Concepto, v: string) => void;
  quitar: (i: number) => void;
}) {
  return (
    <div className="editor">
      {conceptos.map((c, i) => (
        <div key={i} className="concepto">
          <label>
            Palabra {i + 1}
            <input
              required
              maxLength={25}
              value={c.palabra}
              onChange={(e) => cambiar(i, "palabra", e.target.value)}
            />
          </label>
          <label>
            Definición
            <input
              required
              maxLength={1000}
              value={c.definicion}
              onChange={(e) => cambiar(i, "definicion", e.target.value)}
            />
          </label>
          {c.fragmentoFuente && (
            <small className="evidencia">Fuente: “{c.fragmentoFuente}”</small>
          )}
          <button
            type="button"
            className="texto"
            aria-label={`Eliminar concepto ${i + 1}`}
            onClick={() => quitar(i)}
          >
            ×
          </button>
        </div>
      ))}
    </div>
  );
}
