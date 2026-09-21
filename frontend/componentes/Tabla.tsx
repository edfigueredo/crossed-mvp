import type { Fila } from "../tipos";
export function tiempo(ms: number | null | undefined) {
  if (ms == null) return "—";
  const s = Math.floor(ms / 1000);
  return `${Math.floor(s / 60)
    .toString()
    .padStart(2, "0")}:${(s % 60).toString().padStart(2, "0")}`;
}
export default function Tabla({ filas }: { filas: Fila[] }) {
  return (
    <div className="tabla-scroll">
      <table>
        <thead>
          <tr>
            <th>Pos.</th>
            <th>Alumno</th>
            <th>Puntaje</th>
            <th>Tiempo</th>
          </tr>
        </thead>
        <tbody>
          {filas.map((f, i) => (
            <tr
              key={i}
              className={f.pantallaActiva === false ? "inactiva" : ""}
            >
              <td>{f.posicion ?? "—"}</td>
              <td>
                {f.nombre}
                {f.empatado && " · empate"}
                {f.pantallaActiva === false && (
                  <small> Fuera de pantalla</small>
                )}
              </td>
              <td>{f.puntaje ?? "—"}</td>
              <td>{tiempo(f.tiempo)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {!filas.length && (
        <p className="vacio">Los participantes aparecerán acá.</p>
      )}
    </div>
  );
}
