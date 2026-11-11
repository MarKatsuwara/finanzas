import { useNumeroAnimado, useVisto } from '../animaciones'

interface Props {
  valor: number
  formato: (n: number) => string // cómo se escribe: por ejemplo (n) => '+' + euros(n)
  className?: string
}

/** Una cifra que cuenta hasta su valor cuando aparece, y del viejo al nuevo cuando cambia (al cambiar de mes). */
export function Contador({ valor, formato, className }: Props) {
  const { ref, visto } = useVisto<HTMLSpanElement>()
  const mostrado = useNumeroAnimado(valor, visto, 600)
  return (
    <span ref={ref} className={className} aria-label={formato(valor)}>
      <span aria-hidden="true">{formato(mostrado)}</span>
    </span>
  )
}
