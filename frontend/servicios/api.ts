export const API = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
export async function api<T>(
  ruta: string,
  metodo = "GET",
  datos?: unknown,
  token?: string,
): Promise<T> {
  const respuesta = await fetch(API + ruta, {
    method: metodo,
    headers: {
      ...(datos instanceof FormData
        ? {}
        : { "Content-Type": "application/json" }),
      ...(token ? { "X-Token": token } : {}),
    },
    body:
      datos === undefined
        ? undefined
        : datos instanceof FormData
          ? datos
          : JSON.stringify(datos),
  });
  if (!respuesta.ok) {
    const e = await respuesta
      .json()
      .catch(() => ({ mensaje: "No se pudo contactar al servidor." }));
    throw new Error(e.mensaje || "No se pudo completar la operación.");
  }
  const texto = await respuesta.text();
  return (texto ? JSON.parse(texto) : undefined) as T;
}
export function normalizar(s: string) {
  return s
    .toUpperCase()
    .replaceAll("Ñ", "\uE000")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replaceAll("\uE000", "Ñ")
    .replace(/[^A-ZÑ]/g, "");
}
