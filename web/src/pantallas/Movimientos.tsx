import { useState } from 'react'
import type { Movimiento } from '../api'
import { FormularioMovimiento } from '../componentes/FormularioMovimiento'
import { GraficoMeses } from '../componentes/GraficoMeses'
import { ListaMovimientos } from '../componentes/ListaMovimientos'
import { TarjetaCategorias } from '../componentes/TarjetaCategorias'
import { mesActual, moverMes, nombreMesYAnio } from '../formato'

/**
 * La pantalla de movimientos:
 *  1. Apuntar (lo que más se hace, arriba del todo). Al pulsar "Editar" en la lista, se convierte en "Editar movimiento"
 *  2. El gráfico de los últimos 6 meses (al pulsar un mes, la lista salta a él)
 *  3. La lista del mes + en qué categorías se ha ido el dinero
 */
export function Movimientos() {
  const [mes, setMes] = useState(mesActual())
  const [version, setVersion] = useState(0) // sube cada vez que se apunta o se borra algo
  const actualizar = () => setVersion((v) => v + 1)
  const [editando, setEditando] = useState<Movimiento | null>(null) // el que se está modificando

  return (
    <>
      <div className="titulo">
        <div>
          <h1>Movimientos</h1>
          <p>Lo que has ganado y gastado, día a día</p>
        </div>

        {/* Cambiar de mes: ‹ noviembre de 2026 › */}
        <div className="selector-mes">
          <button className="boton boton-icono" onClick={() => setMes(moverMes(mes, -1))} aria-label="Mes anterior">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
              <path d="M10 3 5 8l5 5" />
            </svg>
          </button>
          <span className="selector-mes-nombre" aria-live="polite">{nombreMesYAnio(mes)}</span>
          <button className="boton boton-icono" onClick={() => setMes(moverMes(mes, 1))} aria-label="Mes siguiente">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
              <path d="m6 3 5 5-5 5" />
            </svg>
          </button>
        </div>
      </div>

      <div className="inicio">
        {/* key: cada vez que cambias de movimiento, el formulario se crea de nuevo con sus datos */}
        <FormularioMovimiento
          key={editando?.id ?? 'nuevo'}
          editando={editando}
          onApuntado={actualizar}
          onTerminarEdicion={() => setEditando(null)}
        />

        <GraficoMeses mes={mes} version={version} onElegir={setMes} />

        <div className="movimientos-fila">
          <ListaMovimientos
            mes={mes}
            version={version}
            onCambio={actualizar}
            onEditar={setEditando}
            editandoId={editando?.id ?? null}
          />
          <TarjetaCategorias mes={mes} version={version} />
        </div>
      </div>
    </>
  )
}
