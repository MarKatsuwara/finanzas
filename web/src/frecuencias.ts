import type { Frecuencia } from './api'

/** Cada frecuencia, con su nombre en pantalla y cuántas veces se paga al año. */
export const FRECUENCIAS: { valor: Frecuencia; nombre: string; vecesAlAnio: number }[] = [
  { valor: 'SEMANAL', nombre: 'Semanal', vecesAlAnio: 52 },
  { valor: 'CADA_4_SEMANAS', nombre: 'Cada 4 semanas', vecesAlAnio: 13 }, // cada 28 días: 13 veces al año
  { valor: 'MENSUAL', nombre: 'Mensual', vecesAlAnio: 12 },
  { valor: 'BIMESTRAL', nombre: 'Cada 2 meses', vecesAlAnio: 6 },
  { valor: 'TRIMESTRAL', nombre: 'Trimestral', vecesAlAnio: 4 },
  { valor: 'SEMESTRAL', nombre: 'Semestral', vecesAlAnio: 2 },
  { valor: 'ANUAL', nombre: 'Anual', vecesAlAnio: 1 },
]

/** "TRIMESTRAL" → "Trimestral" */
export function nombreFrecuencia(frecuencia: Frecuencia): string {
  return FRECUENCIAS.find((f) => f.valor === frecuencia)?.nombre ?? frecuencia
}

/**
 * Lo que cuesta al mes, SOLO para enseñarlo mientras se escribe en el formulario.
 * (El dato de verdad lo calcula la API en Java con BigDecimal.)
 */
export function aproximarAlMes(importe: number, frecuencia: Frecuencia): number {
  const veces = FRECUENCIAS.find((f) => f.valor === frecuencia)?.vecesAlAnio ?? 12
  return Math.round(((importe * veces) / 12) * 100) / 100
}
