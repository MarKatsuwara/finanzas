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
  const actualizar = () => setVersion(version + 1)

  // Se piden UNA vez y se pasan al resumen y a la lista, así siempre cuadran
  const { datos, error, cargando, recargar } = useDatos<PagoRecurrente[]>('/api/pagos-recurrentes', version)

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
          {datos ? <ListaFijos fijos={datos} onCambio={actualizar} /> : <div />}
          <FormularioFijo onAnadido={actualizar} />
        </div>
      </div>
    </>
  )
}
