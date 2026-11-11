import { useSyncExternalStore } from 'react'

// =========================================================
// ¿Estamos en una pantalla pequeña (móvil o tablet)? Es la misma medida que usa
// index.css para cambiar el menú lateral por la barra de abajo: 1000 px.
// =========================================================

const consulta = window.matchMedia('(max-width: 1000px)')

function suscribir(oyente: () => void) {
  consulta.addEventListener('change', oyente)
  return () => consulta.removeEventListener('change', oyente)
}

export function useEsMovil(): boolean {
  return useSyncExternalStore(suscribir, () => consulta.matches)
}

/** ¿La persona ha pedido en su sistema que haya menos animaciones? */
export function prefiereMenosMovimiento(): boolean {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}
