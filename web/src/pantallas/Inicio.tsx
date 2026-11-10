import { TarjetaSaldo } from '../componentes/TarjetaSaldo'
import { fechaLarga, hoyIso } from '../formato'

/** La pantalla de inicio. De momento solo tiene el saldo; en el paso 2 añadiremos el resto. */
export function Inicio() {
  const hoy = fechaLarga(hoyIso())

  return (
    <>
      <div className="titulo">
        <div>
          <h1>Inicio</h1>
          {/* La primera letra en mayúscula: "Martes, 10 de noviembre" */}
          <p>{hoy.charAt(0).toUpperCase() + hoy.slice(1)}</p>
        </div>
      </div>

      <TarjetaSaldo />
    </>
  )
}
