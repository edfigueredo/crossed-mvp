export type Concepto = {
  palabra: string;
  definicion: string;
  fragmentoFuente?: string;
};
export type Palabra = {
  idPalabra: number;
  definicion: string;
  filaInicial: number;
  columnaInicial: number;
  direccion: string;
  cantidadLetras?: number;
  palabra?: string;
};
export type Grilla = { filas: number; columnas: number; palabras: Palabra[] };
export type Progreso = {
  correcta: boolean;
  bloqueada: boolean;
  intentosRealizados: number;
};
export type Fila = {
  nombre: string;
  puntaje: number | null;
  tiempo: number | null;
  posicion?: number;
  finalizo: boolean;
  pantallaActiva?: boolean;
  empatado?: boolean;
};
export type Vista = {
  idPartida: string;
  codigoVisible: string;
  nombre: string;
  materia: string;
  idioma: string;
  estado: string;
  cantidadJugadores: number;
  cantidadPalabrasGenerada: number;
  mostrarResultados: boolean;
  tension: number;
  crucigrama?: Grilla;
  progreso?: Record<string, Progreso>;
  finalizo?: boolean;
  ranking?: Fila[];
  jugadores?: Fila[];
  segundosRestantes?: number;
  correos?: string[];
};
export type Sesion = {
  idPartida: string;
  codigoVisible: string;
  token: string;
  idJugador?: string;
  profesor: boolean;
};
