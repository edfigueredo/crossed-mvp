const nombres = [
  "CONTENTO",
  "SONRIENTE",
  "NEUTRO",
  "SERIO",
  "SUENO",
  "PREOCUPADO",
  "MIEDO",
  "SE_FUE",
];
export default function Conejo({ estado = 0 }: { estado?: number }) {
  return (
    <div
      className={"ambiente fase-" + estado}
      aria-label={"Conejito " + nombres[estado]}
    >
      <span className="sol">{estado < 3 ? "☀" : "☾"}</span>
      {estado >= 4 &&
        Array.from(
          {
            length: estado === 4 ? 1 : estado === 5 ? 3 : estado === 6 ? 5 : 7,
          },
          (_, i) => (
            <span
              key={i}
              className="ojos"
              style={{
                left: 7 + i * 13 + "%",
                top: 30 + (i % 3) * 19 + "%",
                animationDelay: i * 0.4 + "s",
              }}
            >
              ▪ ▪
            </span>
          ),
        )}
      {estado < 7 ? (
        <svg
          className="conejo"
          viewBox="0 0 80 90"
          role="img"
          aria-label={nombres[estado]}
          shapeRendering="crispEdges"
        >
          <path
            fill="#f8fafc"
            d="M18 4h12v31h18V4h12v38h8v33H12V42h6zM6 68h68v13H6z"
          />
          <path fill="#f8bbcc" d="M22 9h4v22h-4zM52 9h4v22h-4z" />
          <path
            fill="#172554"
            d={
              estado === 4
                ? "M23 51h12v3H23zM47 51h12v3H47z"
                : "M25 46h7v10h-7zM49 46h7v10h-7z"
            }
          />
          <path fill="#ed8da4" d="M36 59h9v6h-9z" />
          <path
            fill="none"
            stroke="#172554"
            strokeWidth="3"
            d={
              estado < 2
                ? "M30 67v5h22v-5"
                : estado < 5
                  ? "M32 72h18"
                  : "M32 74v-7h18v7"
            }
          />
        </svg>
      ) : (
        <span className="ausente">El conejo dejó las llaves…</span>
      )}
      <div className="suelo" />
    </div>
  );
}
