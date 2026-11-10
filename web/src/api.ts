// =========================================================
// Todo lo que habla con la API en Java está en este archivo.
// =========================================================

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
  let respuesta: Response
  try {
    respuesta = await fetch(ruta, opciones)
  } catch {
    // fetch solo falla así cuando no hay nadie al otro lado
    throw new ErrorApi(0, 'No se puede conectar con la API. ¿Está encendida en IntelliJ?')
  }

  // La web y la API funcionan juntas, pero si la API está apagada el proxy de Vite responde 502/504
  if (respuesta.status === 502 || respuesta.status === 504) {
    throw new ErrorApi(0, 'No se puede conectar con la API. ¿Está encendida en IntelliJ?')
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
