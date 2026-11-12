import { enviar, RUTA_APUNTAR_FIJOS, type Movimiento } from './api'
import { avisar } from './avisos'
import { avisarCambio } from './cambios'
import { euros, hoyIso } from './formato'

// =========================================================
// Los gastos fijos que se apuntan SOLOS.
// Al abrir la app, le pedimos a la API que apunte los fijos que ya han llegado a su día.
// La API se encarga de no apuntar nada dos veces; aquí solo avisamos de lo que ha apuntado.
// =========================================================

let ultimoDia: string | null = null // el último día que lo hemos pedido
let pidiendo = false

/**
 * Pide a la API que apunte los fijos pendientes (como mucho una vez al día).
 * Si ha apuntado algo, recarga los datos y enseña un aviso.
 */
export async function apuntarFijosPendientes(): Promise<void> {
  const hoy = hoyIso()
  if (pidiendo || ultimoDia === hoy) return
  pidiendo = true
  try {
    const nuevos = await enviar<Movimiento[]>(RUTA_APUNTAR_FIJOS, 'POST')
    ultimoDia = hoy
    if (nuevos.length > 0) {
      avisarCambio() // todas las tarjetas se recargan con los movimientos nuevos
      avisar(textoDelAviso(nuevos))
    }
  } catch {
    // Si falla (sin conexión, límite de movimientos...), no molestamos: se reintentará la próxima vez
  } finally {
    pidiendo = false
  }
}

/** "Apuntado solo: Netflix −12,99 €" o "Apuntados solos: Alquiler, Netflix y 2 más". */
function textoDelAviso(nuevos: Movimiento[]): string {
  if (nuevos.length === 1) {
    const m = nuevos[0]
    return `Apuntado solo: ${m.descripcion} ${m.tipo === 'INGRESO' ? '+' : '−'}${euros(m.importe)}`
  }
  const nombres = [...new Set(nuevos.map((m) => m.descripcion))] // sin repetir (dos alquileres = "Alquiler")
  const primeros = nombres.slice(0, 2)
  const resto = nombres.length - primeros.length
  const lista = resto > 0 ? `${primeros.join(', ')} y ${resto} más` : primeros.join(' y ')
  return `Apuntados solos: ${lista}`
}

/**
 * Llamarlo una vez al entrar. Además, si dejas la app abierta y vuelves otro día
 * (por ejemplo, la pestaña del móvil), lo vuelve a pedir al volver a la pestaña.
 * Devuelve la función para dejar de escuchar.
 */
export function vigilarFijosPendientes(): () => void {
  ultimoDia = null // otra sesión (quizá otra cuenta): hay que volver a pedirlo
  void apuntarFijosPendientes()
  const alVolver = () => {
    if (document.visibilityState === 'visible') void apuntarFijosPendientes()
  }
  document.addEventListener('visibilitychange', alVolver)
  return () => document.removeEventListener('visibilitychange', alVolver)
}
