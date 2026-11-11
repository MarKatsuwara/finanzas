import { useEffect, useState } from 'react'
import { alCambiarDespertando, servidorDespertando } from '../api'

/**
 * Un aviso arriba de la pantalla mientras el servidor se despierta.
 * (En el plan gratis, la API se duerme si nadie la usa y tarda 2 o 3 minutos en volver.)
 */
export function AvisoDespertando() {
  const [visible, setVisible] = useState(servidorDespertando)

  useEffect(() => alCambiarDespertando(() => setVisible(servidorDespertando())), [])

  if (!visible) return null
  return (
    <div className="aviso-despertando" role="status">
      <span className="aviso-despertando-punto" aria-hidden="true" />
      El servidor se está despertando. Puede tardar 2 o 3 minutos…
    </div>
  )
}
