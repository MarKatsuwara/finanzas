import { useSyncExternalStore } from 'react'
import type { Movimiento, PagoRecurrente } from './api'

// =========================================================
// Qué hoja está abierta ahora mismo (la ventana que sube desde abajo en el móvil,
// o aparece en el centro en el ordenador). Solo puede haber una a la vez.
// Cualquier parte de la app puede abrir una con abrirHoja(...).
// =========================================================

export type HojaAbierta =
  | { tipo: 'movimiento'; editando?: Movimiento }  // apuntar uno nuevo, o editar ese
  | { tipo: 'fijo'; editando?: PagoRecurrente }    // añadir un fijo, o editar ese
  | { tipo: 'cuenta' }                             // tu cuenta y el tema (solo en el móvil)

let actual: HojaAbierta | null = null
const oyentes = new Set<() => void>()

function cambiar(nueva: HojaAbierta | null) {
  actual = nueva
  oyentes.forEach((oyente) => oyente())
}

export const abrirHoja = (hoja: HojaAbierta) => cambiar(hoja)
export const cerrarHoja = () => cambiar(null)

function suscribir(oyente: () => void) {
  oyentes.add(oyente)
  return () => {
    oyentes.delete(oyente)
  }
}

export function useHojaAbierta(): HojaAbierta | null {
  return useSyncExternalStore(suscribir, () => actual)
}
