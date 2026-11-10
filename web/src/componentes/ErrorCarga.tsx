interface Props {
  mensaje: string
  onReintentar: () => void
}

/** Lo que enseña una tarjeta cuando no ha podido cargar sus datos. */
export function ErrorCarga({ mensaje, onReintentar }: Props) {
  return (
    <div className="estado">
      <p className="aviso-error">{mensaje}</p>
      <button className="boton" onClick={onReintentar}>Reintentar</button>
    </div>
  )
}
