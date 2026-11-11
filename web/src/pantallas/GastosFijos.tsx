import { useState } from 'react'
import type { PagoRecurrente } from '../api'
import { ErrorCarga } from '../componentes/ErrorCarga'
import { FormularioFijo } from '../componentes/FormularioFijo'
import { ListaFijos } from '../componentes/ListaFijos'
import { ResumenFijos } from '../componentes/ResumenFijos'
import { useDatos } from '../useDatos'

/**
 * La pantalla de gastos fijos. Responde a: "¿cuánto me cuestan mis fijos al mes?"
 *  1. Resumen: lo que te cuestan al mes y lo que te queda libre
 *  2. La lista (con interruptor y borrar) + el formulario para añadir
 */
export function GastosFijos() {
  const [version, setVersion] = useState(0)
  const actualizar = () => setVersion((v) => v + 1)
  const [editando, setEditando] = useState<PagoRecurrente | null>(null) // el que se está modificando

  // Se piden UNA vez y se pasan al resumen y a la lista, así siempre cuadran
  const { datos, error, cargando, recargar } = useDatos<PagoRecurrente[]>('/api/pagos-recurrentes', version)

  // Al formulario le pasamos SIEMPRE la versión más reciente del que editas: si mientras tanto
  // le cambias el interruptor, al guardar no se pierde (y si ya no existe, el formulario vuelve a "Añadir").
  const editandoActual = editando ? (datos?.find((f) => f.id === editando.id) ?? null) : null

  return (
    <>
      <div className="titulo">
        <div>
          <h1>Gastos fijos</h1>
          <p>Lo que pagas y cobras cada cierto tiempo</p>
        </div>
      </div>

      <div className="inicio">
        {cargando && !datos && (
          <section className="tarjeta" aria-busy="true">
            <span className="cargando" style={{ width: 240, height: 40 }} />
          </section>
        )}
        {error && (
          <section className="tarjeta">
            <ErrorCarga mensaje={error.message} onReintentar={recargar} />
          </section>
        )}

        {datos && <ResumenFijos fijos={datos} />}

        <div className="movimientos-fila">
          {datos ? (
            <ListaFijos fijos={datos} onCambio={actualizar} onEditar={setEditando} editandoId={editandoActual?.id ?? null} />
          ) : (
            <div />
          )}
          {/* key: cada vez que cambias de fijo, el formulario se crea de nuevo con sus datos */}
          <FormularioFijo
            key={editandoActual?.id ?? 'nuevo'}
            editando={editandoActual}
            onAnadido={actualizar}
            onTerminarEdicion={() => setEditando(null)}
          />
        </div>
      </div>
    </>
  )
}
