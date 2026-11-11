import type { CSSProperties, ReactNode } from 'react'
import { abrirHoja } from '../hojas'

export interface Destino {
  id: string
  nombre: string
  nombreCorto: string // en la barra de abajo cabe poco: "Fijos" en vez de "Gastos fijos"
  icono: ReactNode
}

interface Props {
  destinos: Destino[]          // las 3 pantallas
  actual: string | null        // la pantalla abierta (null en Privacidad)
  inicial: string              // la inicial de tu nombre, para el botón de tu cuenta
}

/**
 * La barra de abajo del móvil, donde llega el pulgar (como en las apps del banco):
 *   Inicio · Movimientos · [ + ] · Fijos · Tú
 * El "+" de en medio apunta un movimiento (o añade un fijo, si estás en Gastos fijos).
 * El indicador de la pestaña elegida se desliza de una a otra (index.css: .barra-indicador).
 */
export function BarraInferior({ destinos, actual, inicial }: Props) {
  // Las 5 casillas: las dos primeras pantallas, el "+", la tercera pantalla y tu cuenta
  const casillas = [destinos[0], destinos[1], null, destinos[2]]
  const posicion = casillas.findIndex((d) => d?.id === actual) // -1 si no hay ninguna (Privacidad)

  function botonMas() {
    abrirHoja(actual === 'fijos' ? { tipo: 'fijo' } : { tipo: 'movimiento' })
  }

  return (
    <nav className="barra-inferior" aria-label="Pantallas">
      {/* El indicador que se mueve a la pestaña elegida. --posicion le dice a qué casilla ir */}
      {posicion >= 0 && (
        <span className="barra-indicador" aria-hidden="true" style={{ '--posicion': posicion } as CSSProperties} />
      )}

      {casillas.map((d, i) =>
        d === null ? (
          <button
            key="mas"
            type="button"
            className="barra-mas"
            onClick={botonMas}
            aria-label={actual === 'fijos' ? 'Añadir fijo' : 'Apuntar movimiento'}
          >
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <path d="M12 5v14M5 12h14" strokeLinecap="round" />
            </svg>
          </button>
        ) : (
          <a
            key={d.id}
            href={`#${d.id}`}
            className="barra-item"
            aria-current={i === posicion ? 'page' : undefined}
          >
            {d.icono}
            <span>{d.nombreCorto}</span>
          </a>
        ),
      )}

      <button type="button" className="barra-item" onClick={() => abrirHoja({ tipo: 'cuenta' })} aria-label="Tú: cuenta y tema">
        <span className="barra-inicial" aria-hidden="true">{inicial}</span>
        <span>Tú</span>
      </button>
    </nav>
  )
}
