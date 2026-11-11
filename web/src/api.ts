// =========================================================
// Todo lo que habla con la API en Java está en este archivo.
// =========================================================
import { cerrarSesion, guardarSesion, sesionActual, type Sesion } from './sesion'

/** Donde se cambia el carné de Google por la pulsera de la API. */
const RUTA_ENTRAR = '/api/auth/google'

/** Un error que viene de la API, con su código (404, 400...) y su mensaje. */
export class ErrorApi extends Error {
  estado: number

  constructor(estado: number, mensaje: string) {
    super(mensaje)
    this.estado = estado
  }
}

/**
 * Hace una petición a la API y devuelve la respuesta ya convertida desde JSON.
 * Si la API responde con error, lanza un ErrorApi con el mensaje que escribimos en Java.
 */
async function peticion<T>(ruta: string, opciones?: RequestInit): Promise<T> {
  // Si has entrado, cada petición lleva tu pulsera: "Authorization: Bearer <token>"
  const cabeceras = new Headers(opciones?.headers)
  const sesion = sesionActual()
  if (sesion) {
    cabeceras.set('Authorization', `Bearer ${sesion.token}`)
  }

  let respuesta: Response
  try {
    respuesta = await fetch(ruta, { ...opciones, headers: cabeceras })
  } catch {
    // fetch solo falla así cuando no hay nadie al otro lado
    throw new ErrorApi(0, 'No se puede conectar con la API. ¿Está encendida en IntelliJ?')
  }

  // La web y la API funcionan juntas, pero si la API está apagada el proxy de Vite responde 502/504
  if (respuesta.status === 502 || respuesta.status === 504) {
    throw new ErrorApi(0, 'No se puede conectar con la API. ¿Está encendida en IntelliJ?')
  }

  // 401 = la pulsera no vale (ha caducado). Cerramos la sesión y la app vuelve a la pantalla de entrar.
  // (Menos al entrar con Google: ahí un 401 significa que el carné de Google no valía.)
  if (respuesta.status === 401 && ruta !== RUTA_ENTRAR) {
    cerrarSesion()
    throw new ErrorApi(401, 'Tu sesión ha caducado. Vuelve a entrar.')
  }

  if (!respuesta.ok) {
    // Nuestros errores llegan con un "detail" (y a veces "errores" por campo)
    const cuerpo = await respuesta.json().catch(() => null)
    const errores = cuerpo?.errores ? Object.values(cuerpo.errores).join('. ') : null
    throw new ErrorApi(respuesta.status, errores ?? cuerpo?.detail ?? `Error ${respuesta.status}`)
  }

  if (respuesta.status === 204) {
    return undefined as T // 204 = todo bien, pero sin nada que devolver (por ejemplo, al borrar)
  }
  return respuesta.json() as Promise<T>
}

/** GET: pedir datos. */
export function pedir<T>(ruta: string): Promise<T> {
  return peticion<T>(ruta)
}

/** Entra con el carné de Google: la API lo comprueba y nos da la pulsera, que guardamos. */
export async function entrarConGoogle(credencial: string): Promise<void> {
  guardarSesion(await enviar<Sesion>(RUTA_ENTRAR, 'POST', { credencial }))
}

/** POST, PUT o DELETE: enviar datos. */
export function enviar<T>(ruta: string, metodo: 'POST' | 'PUT' | 'DELETE', cuerpo?: unknown): Promise<T> {
  return peticion<T>(ruta, {
    method: metodo,
    headers: { 'Content-Type': 'application/json' },
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  })
}

// =========================================================
// Los "tipos": la forma de los datos que devuelve la API.
// Son como los records de Java (SaldoActualResponse, etc.).
// =========================================================

export interface SaldoApuntado {
  id: number
  fecha: string // "2026-11-10"
  importe: number
}

export interface SaldoActual {
  hoy: string
  saldoApuntado: SaldoApuntado
  ingresosDesde: number
  gastosDesde: number
  saldoHoy: number
}

/** Algo que va a entrar o salir un día (de /api/saldo-banco/prevision). */
export interface MovimientoPrevisto {
  descripcion: string
  tipo: 'INGRESO' | 'GASTO'
  importe: number
  origen: 'PAGO_RECURRENTE' | 'MOVIMIENTO'
}

/** Un día con movimientos y cómo queda el saldo al acabar ese día. */
export interface DiaPrevisto {
  fecha: string
  movimientos: MovimientoPrevisto[]
  saldoAlFinal: number
}

/** La previsión del saldo del banco, día a día. */
export interface PrevisionBanco {
  hoy: string
  saldoHoy: number
  hasta: string
  dias: DiaPrevisto[]
  saldoFinal: number
  saldoMinimo: number
  primerDiaEnNegativo: string | null
  numerosRojos: boolean
  mensaje: string
}

/** Un pago recurrente que toca en una fecha concreta. */
export interface ProximoPago {
  fecha: string
  pagoRecurrenteId: number
  descripcion: string
  tipo: 'INGRESO' | 'GASTO'
  importe: number
  categoria: { id: number; nombre: string } | null
}

/** Cómo vas a acabar el mes (de /api/prevision). */
export interface PrevisionMes {
  mes: string // "2026-11"
  hoy: string
  ingresosApuntados: number
  gastosApuntados: number
  ingresosPendientes: number
  gastosPendientes: number
  saldoPrevisto: number
  numerosRojos: boolean
  mensaje: string
  pagosPendientes: ProximoPago[]
}

export interface Categoria {
  id: number
  nombre: string
}

/** Un ingreso o un gasto apuntado (de /api/movimientos). */
export interface Movimiento {
  id: number
  tipo: 'INGRESO' | 'GASTO'
  importe: number
  fecha: string
  descripcion: string
  categoria: Categoria | null
}

/** Cuánto se ha ingresado o gastado en una categoría (categoriaId es null en "Sin categoría"). */
export interface TotalCategoria {
  categoriaId: number | null
  categoria: string
  total: number
}

/** El resumen de un mes (de /api/resumen?mes=2026-11). */
export interface ResumenMes {
  mes: string
  totalIngresos: number
  totalGastos: number
  saldo: number
  ingresosPorCategoria: TotalCategoria[]
  gastosPorCategoria: TotalCategoria[]
}

export type Frecuencia = 'SEMANAL' | 'CADA_4_SEMANAS' | 'MENSUAL' | 'BIMESTRAL' | 'TRIMESTRAL' | 'SEMESTRAL' | 'ANUAL'

/** Un gasto o ingreso fijo (de /api/pagos-recurrentes). */
export interface PagoRecurrente {
  id: number
  descripcion: string
  tipo: 'INGRESO' | 'GASTO'
  importe: number
  frecuencia: Frecuencia
  fechaInicio: string
  activo: boolean
  categoria: Categoria | null
  importeMensual: number       // lo que cuesta repartido por meses
  proximaFecha: string | null  // null si está desactivado
}
