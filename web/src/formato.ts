// =========================================================
// Cómo se escriben los números y las fechas en pantalla (en español).
// =========================================================

const EUROS = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' })

/** 1501.26 → "1.501,26 €" */
export function euros(cantidad: number): string {
  return EUROS.format(cantidad)
}

/**
 * Separa una cantidad en dos trozos para pintar los céntimos más pequeños:
 * 1501.26 → { entero: "1.501", resto: ",26 €" }
 */
export function partesDeEuros(cantidad: number): { entero: string; resto: string } {
  let entero = ''
  let resto = ''
  for (const parte of EUROS.formatToParts(cantidad)) {
    if (parte.type === 'minusSign' || parte.type === 'integer' || parte.type === 'group') {
      entero += parte.value
    } else {
      resto += parte.value
    }
  }
  return { entero, resto }
}

/** "2026-11-10" → un objeto Date (sin líos de zonas horarias) */
function aFecha(iso: string): Date {
  const [anio, mes, dia] = iso.split('-').map(Number)
  return new Date(anio, mes - 1, dia)
}

/** "2026-11-10" → "martes, 10 de noviembre" */
export function fechaLarga(iso: string): string {
  return aFecha(iso).toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long' })
}

/** "2026-11-10" → "10 nov" */
export function fechaCorta(iso: string): string {
  return aFecha(iso).toLocaleDateString('es-ES', { day: 'numeric', month: 'short' }).replace('.', '')
}

/** La fecha de hoy en formato "2026-11-10" (el que entiende la API) */
export function hoyIso(): string {
  const hoy = new Date()
  const mes = String(hoy.getMonth() + 1).padStart(2, '0')
  const dia = String(hoy.getDate()).padStart(2, '0')
  return `${hoy.getFullYear()}-${mes}-${dia}`
}

/** Convierte lo que escribe la persona ("12,50" o "12.50") en número. NaN si no es un número. */
export function leerImporte(texto: string): number {
  return Number(texto.trim().replace(',', '.'))
}
