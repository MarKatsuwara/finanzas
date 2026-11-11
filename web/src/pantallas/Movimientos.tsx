import { useState } from 'react'
import type { Movimiento } from '../api'
import { abrirHoja } from '../hojas'
import { useEsMovil } from '../movil'
import { FormularioMovimiento } from '../componentes/FormularioMovimiento'
import { GraficoMeses } from '../componentes/GraficoMeses'
import { ListaMovimientos } from '../componentes/ListaMovimientos'
import { TarjetaCategorias } from '../componentes/TarjetaCategorias'
import { mesActual, moverMes, nombreMesYAnio } from '../formato'

/**
 * La pantalla de movimientos:
 *  1. Apuntar (lo que más se hace, arriba del todo). Al pulsar una fila, se convierte en "Editar movimiento".
 *     En el móvil no se ve: se apunta con el "+" de la barra de abajo y se edita en una hoja que sube
 *  2. El gráfico de los últimos 6 meses (al pulsar un mes, la lista salta a él)
 *  3. La lista del mes + en qué categorías se ha ido el dinero
 */
export function Movimientos() {
  const [mes, setMes] = useState(mesActual())
  const [direccion, setDireccion] = useState(0) // hacia dónde has cambiado de mes (para la animación de la lista)
  const irAlMes = (nuevo: string) => {
    setDireccion(nuevo > mes ? 1 : nuevo < mes ? -1 : 0) // "2026-12" > "2026-11": se pueden comparar como texto
    setMes(nuevo)
  }
  const [editando, setEditando] = useState<Movimiento | null>(null) // el que se está modificando (en el ordenador)
  const esMovil = useEsMovil()
  // Pulsar una fila: en el móvil abre la hoja; en el ordenador rellena el formulario de arriba
  const editar = (m: Movimiento) => (esMovil ? abrirHoja({ tipo: 'movimiento', editando: m }) : setEditando(m))

  return (
    <>
      <div className="titulo">
        <div>
          <h1>Movimientos</h1>
          <p>Lo que has ganado y gastado, día a día</p>
        </div>

        {/* Cambiar de mes: ‹ noviembre de 2026 › */}
        <div className="selector-mes">
          <button className="boton boton-icono" onClick={() => irAlMes(moverMes(mes, -1))} aria-label="Mes anterior">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
              <path d="M10 3 5 8l5 5" />
            </svg>
          </button>
          <span className="selector-mes-nombre" aria-live="polite">{nombreMesYAnio(mes)}</span>
          <button className="boton boton-icono" onClick={() => irAlMes(moverMes(mes, 1))} aria-label="Mes siguiente">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
              <path d="m6 3 5 5-5 5" />
            </svg>
          </button>
        </div>
      </div>

      <div className="inicio">
        {/* key: cada vez que cambias de movimiento, el formulario se crea de nuevo con sus datos */}
        {!esMovil && (
          <FormularioMovimiento
            key={editando?.id ?? 'nuevo'}
            editando={editando}
            onTerminarEdicion={() => setEditando(null)}
          />
        )}

        <GraficoMeses mes={mes} onElegir={irAlMes} />

        <div className="movimientos-fila">
          <ListaMovimientos
            mes={mes}
            onEditar={editar}
            editandoId={editando?.id ?? null}
            direccion={direccion}
          />
          <TarjetaCategorias mes={mes} />
        </div>
      </div>
    </>
  )
}
