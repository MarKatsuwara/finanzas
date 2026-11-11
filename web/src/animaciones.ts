import { useEffect, useRef, useState } from 'react'
import { prefiereMenosMovimiento } from './movil'

// =========================================================
// Ayudantes para las animaciones (sin librerías: solo React y el navegador).
// =========================================================

/**
 * ¿Ya ha aparecido en pantalla? Devuelve un "ref" para ponérselo al elemento y "visto",
 * que pasa a true la primera vez que se ve (y ya no vuelve a false).
 * Así lo de abajo del todo se anima cuando bajas hasta ello, no antes.
 */
export function useVisto<T extends Element>() {
  const ref = useRef<T>(null)
  const [visto, setVisto] = useState(false)

  useEffect(() => {
    const elemento = ref.current
    if (!elemento) return
    // Navegador muy antiguo o "menos movimiento": se enseña ya
    if (!('IntersectionObserver' in window) || prefiereMenosMovimiento()) {
      const frame = requestAnimationFrame(() => setVisto(true))
      return () => cancelAnimationFrame(frame)
    }
    // IntersectionObserver: el navegador nos avisa cuando el elemento entra en la pantalla
    const observador = new IntersectionObserver(
      ([entrada]) => {
        if (entrada.isIntersecting) {
          setVisto(true)
          observador.disconnect()
        }
      },
      { threshold: 0.15 }, // cuando se ve al menos un 15%
    )
    observador.observe(elemento)
    return () => observador.disconnect()
  }, [])

  return { ref, visto }
}

/** Sale rápido y frena al final (igual que la --curva de index.css) */
const frenar = (t: number) => 1 - Math.pow(1 - t, 3)

/**
 * Un número que "cuenta" hasta su valor: 0 → 1.279,35 la primera vez, y del valor viejo al nuevo
 * cuando cambia (por ejemplo, al apuntar un gasto, el saldo baja contando).
 * "activo": hasta que no es true, se queda en el valor de partida (para esperar a que se vea).
 */
export function useNumeroAnimado(objetivo: number, activo = true, duracion = 800): number {
  const [valor, setValor] = useState(() => (prefiereMenosMovimiento() ? objetivo : 0))
  const actual = useRef(valor) // por dónde va (para empezar desde ahí si cambia a mitad)

  useEffect(() => {
    if (!activo) return
    const origen = actual.current
    if (origen === objetivo) return
    const total = prefiereMenosMovimiento() ? 0 : duracion
    const inicio = performance.now()
    let frame = 0

    // requestAnimationFrame: "llámame en el próximo fotograma" (unas 60 veces por segundo)
    const paso = (ahora: number) => {
      const progreso = total === 0 ? 1 : Math.min(1, (ahora - inicio) / total)
      const siguiente = progreso === 1 ? objetivo : origen + (objetivo - origen) * frenar(progreso)
      actual.current = siguiente
      setValor(siguiente)
      if (progreso < 1) frame = requestAnimationFrame(paso)
    }
    frame = requestAnimationFrame(paso)
    return () => cancelAnimationFrame(frame)
  }, [objetivo, activo, duracion])

  // Redondeado a céntimos, para que nunca se vea 1.279,3487 €
  return Math.round(valor * 100) / 100
}
