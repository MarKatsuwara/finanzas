import { useEffect, useRef, useState, type PointerEvent, type ReactNode } from 'react'
import { prefiereMenosMovimiento } from '../movil'

interface Props {
  titulo: string
  onCerrar: () => void   // se llama cuando la hoja ya ha terminado de cerrarse
  children: (cerrar: () => void) => ReactNode // el contenido recibe "cerrar" para poder cerrarla al guardar
}

/** Lo que dura la animación de cerrar (tiene que coincidir con index.css: .hoja.cerrando) */
const DURACION_CERRAR = 220

/**
 * Una "hoja": en el móvil sube desde abajo (como en las apps del banco) y en el ordenador
 * aparece en el centro. Usa <dialog>, que el navegador ya sabe manejar:
 *  - showModal() la abre por encima de todo y no deja usar lo de detrás;
 *  - la tecla Esc la cierra; el tabulador no se escapa fuera de ella.
 * En el móvil, además, se puede cerrar arrastrándola hacia abajo con el dedo.
 */
export function Hoja({ titulo, onCerrar, children }: Props) {
  const dialogo = useRef<HTMLDialogElement>(null)
  const [cerrando, setCerrando] = useState(false)
  const [arrastre, setArrastre] = useState(0) // cuántos px la has bajado con el dedo
  const inicioArrastre = useRef<number | null>(null)

  // Al aparecer, se abre, y el cursor salta al primer campo (en el móvil, sale el teclado ya)
  useEffect(() => {
    const d = dialogo.current
    if (!d || d.open) return
    d.showModal()
    d.querySelector<HTMLElement>('.hoja-cuerpo input, .hoja-cuerpo select')?.focus()
  }, [])

  function cerrar() {
    if (cerrando) return
    if (prefiereMenosMovimiento()) {
      dialogo.current?.close()
      onCerrar()
      return
    }
    setCerrando(true) // index.css le pone la animación de salida
    setTimeout(() => {
      dialogo.current?.close()
      onCerrar()
    }, DURACION_CERRAR)
  }

  // ---- Arrastrar hacia abajo para cerrar (solo tiene sentido en el móvil) ----
  function empezar(e: PointerEvent) {
    if ((e.target as Element).closest('button')) return // en la ✕ no se arrastra, se pulsa
    if (!window.matchMedia('(max-width: 1000px)').matches) return // en el ordenador no se arrastra
    inicioArrastre.current = e.clientY
    ;(e.target as Element).setPointerCapture(e.pointerId)
  }
  function mover(e: PointerEvent) {
    if (inicioArrastre.current === null) return
    setArrastre(Math.max(0, e.clientY - inicioArrastre.current)) // hacia arriba no se mueve
  }
  function soltar() {
    if (inicioArrastre.current === null) return
    inicioArrastre.current = null
    if (arrastre > 90) cerrar() // si la has bajado bastante, se cierra
    setArrastre(0)              // si no, vuelve a su sitio
  }

  return (
    <dialog
      ref={dialogo}
      className={cerrando ? 'hoja cerrando' : 'hoja'}
      aria-labelledby="hoja-titulo"
      // Esc: en vez de cerrarse de golpe, hacemos la animación
      onCancel={(e) => {
        e.preventDefault()
        cerrar()
      }}
      // Pulsar fuera de la hoja (en el fondo oscuro) también la cierra
      onClick={(e) => {
        if (e.target === e.currentTarget) cerrar()
      }}
      style={arrastre > 0 ? { transform: `translateY(${arrastre}px)`, transition: 'none' } : undefined}
    >
      <div
        className="hoja-cabecera"
        onPointerDown={empezar}
        onPointerMove={mover}
        onPointerUp={soltar}
        onPointerCancel={soltar}
      >
        <span className="hoja-asa" aria-hidden="true" />
        <h2 id="hoja-titulo">{titulo}</h2>
        <button type="button" className="boton boton-icono hoja-cerrar" onClick={cerrar} aria-label="Cerrar">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
            <path d="M4 4l8 8M12 4l-8 8" strokeLinecap="round" />
          </svg>
        </button>
      </div>
      <div className="hoja-cuerpo">{children(cerrar)}</div>
    </dialog>
  )
}
