import { useState } from 'react'
import type { PrevisionBanco } from '../api'
import { GraficoSaldo } from '../componentes/GraficoSaldo'
import { TarjetaMes } from '../componentes/TarjetaMes'
import { TarjetaProximos } from '../componentes/TarjetaProximos'
import { TarjetaSaldo } from '../componentes/TarjetaSaldo'
import { fechaLarga, hoyIso } from '../formato'
import { useDatos } from '../useDatos'

/**
 * La pantalla de inicio. Responde a: "¿voy a quedarme sin dinero?"
 *  1. En el banco hoy (con el aviso)
 *  2. El gráfico de cómo irá tu saldo
 *  3. Próximos 30 días (cuánto te queda cada día) + Este mes
 */
export function Inicio() {
  // Cada vez que algo cambia (por ejemplo, cuadras el saldo), sumamos 1
  // y todas las tarjetas vuelven a pedir sus datos.
  const [version, setVersion] = useState(0)
  const prevision = useDatos<PrevisionBanco>('/api/saldo-banco/prevision', version)

  const hoy = fechaLarga(hoyIso())

  return (
    <>
      <div className="titulo">
        <div>
          <h1>Inicio</h1>
          {/* La primera letra en mayúscula: "Martes, 10 de noviembre" */}
          <p>{hoy.charAt(0).toUpperCase() + hoy.slice(1)}</p>
        </div>
        {/* Lo que más se hace en la app, a un clic */}
        <a className="boton boton-principal" href="#movimientos">Apuntar movimiento</a>
      </div>

      <div className="inicio">
        <TarjetaSaldo version={version} prevision={prevision.datos} onCambio={() => setVersion(version + 1)} />

        <GraficoSaldo {...prevision} />

        <div className="inicio-fila">
          <TarjetaProximos {...prevision} />
          <TarjetaMes version={version} />
        </div>
      </div>
    </>
  )
}
