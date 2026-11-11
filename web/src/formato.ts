// =========================================================
// Cómo se escriben los números y las fechas en pantalla (en español).
// =========================================================

// useGrouping 'always': pone el punto de los miles también en números de 4 cifras (1.501 y no 1501)
const EUROS = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR', useGrouping: 'always' })

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

/** "2026-11-15" → { dia: "15", mes: "nov" } (para el cuadradito de la fecha) */
export function diaYMes(iso: string): { dia: string; mes: string } {
  const fecha = aFecha(iso)
  return {
    dia: String(fecha.getDate()),
    mes: fecha.toLocaleDateString('es-ES', { month: 'short' }).replace('.', ''),
  }
}

/** "2026-11" → "noviembre" */
export function nombreMes(mesIso: string): string {
  return aFecha(`${mesIso}-01`).toLocaleDateString('es-ES', { month: 'long' })
}

/** El mes de hoy en formato "2026-11" */
export function mesActual(): string {
  return hoyIso().slice(0, 7)
}

/** Mueve un mes hacia delante o hacia atrás: moverMes("2026-11", -1) → "2026-10" */
export function moverMes(mesIso: string, cuanto: number): string {
  const [anio, mes] = mesIso.split('-').map(Number)
  const fecha = new Date(anio, mes - 1 + cuanto, 1)
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}`
}

/** "2026-11" → "noviembre de 2026" */
export function nombreMesYAnio(mesIso: string): string {
  return aFecha(`${mesIso}-01`).toLocaleDateString('es-ES', { month: 'long', year: 'numeric' })
}

/** El título de un día en una lista: "Hoy", "Ayer" o "Domingo, 8 nov" */
export function tituloDia(iso: string): string {
  const hoy = hoyIso()
  const ayer = new Date()
  ayer.setDate(ayer.getDate() - 1)
  const ayerIso = `${ayer.getFullYear()}-${String(ayer.getMonth() + 1).padStart(2, '0')}-${String(ayer.getDate()).padStart(2, '0')}`

  if (iso === hoy) return 'Hoy'
  if (iso === ayerIso) return 'Ayer'
  const texto = aFecha(iso).toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'short' }).replace('.', '')
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

/**
 * Suma cantidades de dinero sin errores de decimales.
 * (En JavaScript 0.1 + 0.2 da 0.30000000000000004: por eso sumamos en céntimos.)
 */
export function sumar(cantidades: number[]): number {
  const centimos = cantidades.reduce((total, cantidad) => total + Math.round(cantidad * 100), 0)
  return centimos / 100
}

/** Cuántos días hay de una fecha a otra: diasEntre("2026-11-10", "2026-11-15") → 5 */
export function diasEntre(desde: string, hasta: string): number {
  const UN_DIA = 24 * 60 * 60 * 1000
  const enUtc = (iso: string) => {
    const [anio, mes, dia] = iso.split('-').map(Number)
    return Date.UTC(anio, mes - 1, dia)
  }
  return Math.round((enUtc(hasta) - enUtc(desde)) / UN_DIA)
}

/** Suma días a una fecha: sumarDias("2026-11-30", 2) → "2026-12-02" */
export function sumarDias(iso: string, dias: number): string {
  const fecha = aFecha(iso)
  fecha.setDate(fecha.getDate() + dias)
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`
}

// Para los ejes de los gráficos: sin céntimos ("1.500 €")
const EUROS_REDONDOS = new Intl.NumberFormat('es-ES', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0,
  useGrouping: 'always',
})

/** 1500 → "1.500 €" (sin céntimos, para los ejes de los gráficos) */
export function eurosRedondos(cantidad: number): string {
  return EUROS_REDONDOS.format(cantidad)
}

/** "2026-11" → "nov" (para debajo de las barras) */
export function mesCorto(mesIso: string): string {
  return aFecha(`${mesIso}-01`).toLocaleDateString('es-ES', { month: 'short' }).replace('.', '')
}
