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
