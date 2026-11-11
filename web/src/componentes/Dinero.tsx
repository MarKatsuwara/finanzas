import { useNumeroAnimado, useVisto } from '../animaciones'
import { partesDeEuros } from '../formato'

interface Props {
  cantidad: number
  grande?: boolean      // true = el tamaño del saldo principal
  conSigno?: boolean    // true = pone "+" delante si es positivo
  animado?: boolean     // true = cuenta hasta la cantidad cuando aparece en pantalla (y cuando cambia)
}

/**
 * Pinta una cantidad de dinero con los céntimos más pequeños y en gris: 1.501,26 €
 * Si es negativa, se pinta en rojo.
 */
export function Dinero({ cantidad, grande = false, conSigno = false, animado = false }: Props) {
  const { ref, visto } = useVisto<HTMLSpanElement>()
  const animada = useNumeroAnimado(cantidad, visto)
  const mostrada = animado ? animada : cantidad

  const { entero, resto } = partesDeEuros(mostrada)
  const clases = ['dinero', 'num', grande ? 'dinero-grande' : '', cantidad < 0 ? 'negativo' : '']

  // El lector de pantalla lee la cantidad final, no los números mientras cuentan
  const final = partesDeEuros(cantidad)
  const textoFinal = `${conSigno && cantidad > 0 ? '+' : ''}${final.entero}${final.resto}`

  return (
    <span ref={ref} className={clases.join(' ')} aria-label={animado ? textoFinal : undefined}>
      <span aria-hidden={animado || undefined}>
        {conSigno && mostrada > 0 ? '+' : ''}
        {entero}
        <span className="centimos">{resto}</span>
      </span>
    </span>
  )
}
