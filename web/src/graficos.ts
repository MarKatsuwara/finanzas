// =========================================================
// Herramientas para dibujar gráficos con SVG (sin librerías).
//
// La idea de un gráfico es como el papel cuadriculado del colegio:
// tenemos un rectángulo de X píxeles y hay que "traducir" cada dato
// (un día, una cantidad de euros) a una posición dentro de él.
// =========================================================
import { useEffect, useRef, useState } from 'react'

/**
 * Mide el ancho de un elemento y avisa cada vez que cambia
 * (por ejemplo, al girar el móvil o hacer la ventana más pequeña).
 * Así el gráfico se dibuja siempre al tamaño justo, sin estirar las letras.
 */
export function useAncho<T extends HTMLElement>() {
  const ref = useRef<T>(null)
  const [ancho, setAncho] = useState(0)

  useEffect(() => {
    const elemento = ref.current
    if (!elemento) return
    const observador = new ResizeObserver(([entrada]) => setAncho(Math.floor(entrada.contentRect.width)))
    observador.observe(elemento)
    return () => observador.disconnect()
  }, [])

  return { ref, ancho }
}

/**
 * Una "regla" que convierte un valor en una posición en píxeles.
 * escala(0, 1000, 200, 0) → el 0 va al píxel 200 y el 1000 al píxel 0.
 * (En SVG la Y crece hacia ABAJO, por eso los euros van "al revés".)
 */
export function escala(valorMin: number, valorMax: number, pixelMin: number, pixelMax: number) {
  const rango = valorMax - valorMin || 1
  return (valor: number) => pixelMin + ((valor - valorMin) / rango) * (pixelMax - pixelMin)
}

/**
 * Las rayitas del eje con números "redondos" (0, 500, 1.000...) en vez de 0, 412, 824.
 * Busca un paso de 1, 2, 2,5 o 5 por una potencia de 10 que dé unas "cuantas" marcas.
 */
export function marcasRedondas(min: number, max: number, cuantas = 4): number[] {
  if (max <= min) max = min + 1
  const pasoAproximado = (max - min) / cuantas
  const potencia = 10 ** Math.floor(Math.log10(pasoAproximado))
  const paso = [1, 2, 2.5, 5, 10].map((m) => m * potencia).find((p) => p >= pasoAproximado) ?? potencia * 10

  const desde = Math.floor(min / paso) * paso
  const hasta = Math.ceil(max / paso) * paso
  const marcas: number[] = []
  for (let valor = desde; valor <= hasta + paso / 2; valor += paso) {
    marcas.push(Math.round(valor * 100) / 100)
  }
  return marcas
}

/**
 * El dibujo de una barra con las esquinas de ARRIBA redondeadas y la base recta
 * (la base está "apoyada" en el suelo del gráfico).
 */
export function barraRedondeada(x: number, y: number, ancho: number, alto: number, radio = 4): string {
  if (alto <= 0) return ''
  const r = Math.min(radio, alto, ancho / 2)
  return [
    `M${x},${y + alto}`, // abajo a la izquierda
    `V${y + r}`, // sube
    `Q${x},${y} ${x + r},${y}`, // esquina redondeada
    `H${x + ancho - r}`, // cruza por arriba
    `Q${x + ancho},${y} ${x + ancho},${y + r}`, // otra esquina
    `V${y + alto}`, // baja
    'Z',
  ].join(' ')
}
