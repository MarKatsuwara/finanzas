import { useSyncExternalStore } from 'react'

// =========================================================
// Los avisos que aparecen un momento abajo ("Apuntado: Café −2,80 €") y se van solos.
// =========================================================

export interface Aviso {
  id: number
  texto: string
}

let avisos: Aviso[] = []
let siguienteId = 1
const oyentes = new Set<() => void>()

function cambiar(nuevos: Aviso[]) {
  avisos = nuevos
  oyentes.forEach((oyente) => oyente())
}

/** Enseña un aviso durante unos segundos. */
export function avisar(texto: string) {
  const aviso = { id: siguienteId++, texto }
  cambiar([...avisos, aviso].slice(-2)) // como mucho dos a la vez
  setTimeout(() => cambiar(avisos.filter((a) => a.id !== aviso.id)), 3200)
}

function suscribir(oyente: () => void) {
  oyentes.add(oyente)
  return () => {
    oyentes.delete(oyente)
  }
}

export function useAvisos(): Aviso[] {
  return useSyncExternalStore(suscribir, () => avisos)
}
