// =========================================================
// La sesión: la "pulsera" (token) que nos da la API al entrar con Google.
//
// La guardamos en el navegador (localStorage) para que, si cierras la pestaña
// y vuelves, sigas dentro hasta que caduque (7 días).
// =========================================================

const CLAVE = 'finanzas-sesion'

export interface Usuario {
  id: number
  email: string
  nombre: string
  foto: string | null
}

/** Lo que devuelve POST /api/auth/google. */
export interface Sesion {
  token: string
  caducaEn: string // "2026-11-18T12:00:00Z"
  usuario: Usuario
}

/** Lee la sesión guardada. Si no hay, está rota o ya ha caducado → null. */
function cargar(): Sesion | null {
  try {
    const texto = localStorage.getItem(CLAVE)
    if (!texto) return null
    const sesion = JSON.parse(texto) as Sesion
    if (!sesion.token || new Date(sesion.caducaEn) <= new Date()) {
      localStorage.removeItem(CLAVE)
      return null
    }
    return sesion
  } catch {
    return null // navegador en modo privado, datos corruptos...: como si no hubiera sesión
  }
}

// La sesión de ahora, en memoria. localStorage solo sirve para recordarla al volver.
let actual: Sesion | null = cargar()

// Quien quiera enterarse de que la sesión cambia (la App, para enseñar la pantalla de entrar)
const oyentes = new Set<() => void>()

function avisar() {
  oyentes.forEach((oyente) => oyente())
}

export function sesionActual(): Sesion | null {
  return actual
}

export function guardarSesion(sesion: Sesion) {
  actual = sesion
  try {
    localStorage.setItem(CLAVE, JSON.stringify(sesion))
  } catch {
    // si el navegador no deja guardar, la sesión dura hasta cerrar la pestaña
  }
  avisar()
}

export function cerrarSesion() {
  actual = null
  try {
    localStorage.removeItem(CLAVE)
  } catch {
    // nada que hacer
  }
  avisar()
}

/** Llama a "oyente" cada vez que se entra o se sale. Devuelve una función para dejar de escuchar. */
export function alCambiarLaSesion(oyente: () => void): () => void {
  oyentes.add(oyente)
  return () => {
    oyentes.delete(oyente)
  }
}
