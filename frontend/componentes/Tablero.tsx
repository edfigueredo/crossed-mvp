import type { Grilla, Progreso } from "../tipos";
export default function Tablero({
  grilla,
  activa,
  elegir,
  respuestas = {},
  progreso = {},
  previa = false,
}: {
  grilla: Grilla;
  activa: number;
  elegir: (id: number) => void;
  respuestas?: Record<number, string>;
  progreso?: Record<string, Progreso>;
  previa?: boolean;
}) {
  const celdas = Array.from({ length: grilla.filas * grilla.columnas }, () => ({
    ids: [] as number[],
    letra: "",
    numero: 0,
  }));
  grilla.palabras.forEach((p) => {
    for (let i = 0; i < (p.cantidadLetras ?? p.palabra?.length ?? 0); i++) {
      const f = p.filaInicial + (p.direccion === "VERTICAL" ? i : 0),
        c = p.columnaInicial + (p.direccion === "HORIZONTAL" ? i : 0);
      const celda = celdas[f * grilla.columnas + c];
      celda.ids.push(p.idPalabra);
      if (i === 0)
        celda.numero = celda.numero
          ? Math.min(celda.numero, p.idPalabra)
          : p.idPalabra;
      const letra = previa
        ? p.palabra?.[i]
        : progreso[p.idPalabra]?.correcta
          ? respuestas[p.idPalabra]?.[i]
          : undefined;
      if (letra) celda.letra = letra;
    }
  });
  return (
    <div className="tablero-contenedor">
      <div
        className="tablero"
        style={{
          gridTemplateColumns: `repeat(${grilla.columnas},minmax(20px,1fr))`,
          maxWidth: grilla.columnas * 42,
        }}
      >
        {celdas.map((c, i) =>
          c.ids.length ? (
            <button
              key={i}
              type="button"
              aria-label={`Fila ${Math.floor(i / grilla.columnas) + 1}, columna ${(i % grilla.columnas) + 1}, palabra ${c.ids.join(" y ")}`}
              className={
                "celda " +
                (c.ids.includes(activa) ? "activa " : "") +
                (c.letra ? "resuelta" : "")
              }
              onClick={() =>
                elegir(
                  c.ids.includes(activa)
                    ? c.ids[(c.ids.indexOf(activa) + 1) % c.ids.length]
                    : c.ids[0],
                )
              }
            >
              <small>{c.numero || ""}</small>
              {c.letra}
            </button>
          ) : (
            <span key={i} className="vacia" />
          ),
        )}
      </div>
    </div>
  );
}
