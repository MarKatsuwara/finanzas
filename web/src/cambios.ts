import { useSyncExternalStore } from 'react'

// =========================================================
// "Algo ha cambiado en los datos": un contador que sube cada vez que se guarda o se borra algo.
// useDatos lo escucha, así TODAS las tarjetas de la pantalla se recargan solas
// (por ejemplo, al apuntar un gasto desde la hoja del botón "+").
// =========================================================

let contador = 0
const oyentes = new Set<() => void>()

/** Llamarlo después de guardar o borrar cualquier cosa. */
export function avisarCambio() {
  contador++
  oyentes.forEach((oyente) => oyente())
}

function suscribir(oyente: () => void) {
  oyentes.add(oyente)
  return () => {
    oyentes.delete(oyente)
  }
}

/** Devuelve el contador: cuando cambia, el componente que lo usa se vuelve a pintar. */
export function useCambios(): number {
  return useSyncExternalStore(suscribir, () => contador)
}

// =========================================================
// "Esto es nuevo": lo último que se ha apuntado o añadido, para que su fila brille un momento.
// Es un texto como "mov-57" o "fijo-12" y se olvida solo a los 2 segundos.
// =========================================================

let nuevo: string | null = null
const oyentesNuevo = new Set<() => void>()

export function marcarNuevo(clave: string) {
  nuevo = clave
  oyentesNuevo.forEach((oyente) => oyente())
  setTimeout(() => {
    if (nuevo !== clave) return
    nuevo = null
    oyentesNuevo.forEach((oyente) => oyente())
  }, 2000)
}

function suscribirNuevo(oyente: () => void) {
  oyentesNuevo.add(oyente)
  return () => {
    oyentesNuevo.delete(oyente)
  }
}

export function useNuevo(): string | null {
  return useSyncExternalStore(suscribirNuevo, () => nuevo)
}
