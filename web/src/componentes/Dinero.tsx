import { partesDeEuros } from '../formato'

interface Props {
  cantidad: number
  grande?: boolean      // true = el tamaño del saldo principal
  conSigno?: boolean    // true = pone "+" delante si es positivo
}

/**
 * Pinta una cantidad de dinero con los céntimos más pequeños y en gris: 1.501,26 €
 * Si es negativa, se pinta en rojo.
 */
export function Dinero({ cantidad, grande = false, conSigno = false }: Props) {
  const { entero, resto } = partesDeEuros(cantidad)
  const clases = ['dinero', 'num', grande ? 'dinero-grande' : '', cantidad < 0 ? 'negativo' : '']

  return (
    <span className={clases.join(' ')}>
      {conSigno && cantidad > 0 ? '+' : ''}
      {entero}
      <span className="centimos">{resto}</span>
    </span>
  )
}
