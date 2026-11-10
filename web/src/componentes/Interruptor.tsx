interface Props {
  encendido: boolean
  onCambiar: () => void
  etiqueta: string       // lo que lee un lector de pantalla: "Alquiler activo"
  desactivado?: boolean  // mientras se guarda, no se puede pulsar
}

/** Un interruptor de encendido / apagado, como los del móvil. */
export function Interruptor({ encendido, onCambiar, etiqueta, desactivado = false }: Props) {
  return (
    <button
      type="button"
      role="switch"            // le dice al navegador que es un interruptor
      aria-checked={encendido}  // y si está encendido o no
      aria-label={etiqueta}
      className="interruptor"
      onClick={onCambiar}
      disabled={desactivado}
    >
      <span className="interruptor-bola" aria-hidden="true" />
    </button>
  )
}
