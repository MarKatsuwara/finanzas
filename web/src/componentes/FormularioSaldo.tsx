import { useState, type FormEvent } from 'react'
import { enviar, ErrorApi } from '../api'
import { hoyIso, leerImporte } from '../formato'

interface Props {
  onGuardado: () => void   // qué hacer cuando se guarda bien (normalmente: recargar el saldo)
  onCancelar?: () => void  // si se pasa, aparece el botón "Cancelar"
}

/** Formulario para apuntar cuánto dinero tienes en el banco. */
export function FormularioSaldo({ onGuardado, onCancelar }: Props) {
  // useState guarda lo que la persona va escribiendo en cada campo
  const [importe, setImporte] = useState('')
  const [fecha, setFecha] = useState(hoyIso())
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  async function guardar(evento: FormEvent) {
    evento.preventDefault() // que el navegador no recargue la página al enviar
    const cantidad = leerImporte(importe)
    if (importe.trim() === '' || Number.isNaN(cantidad)) {
      setError('Escribe una cantidad, por ejemplo 400 o 400,50')
      return
    }

    setGuardando(true)
    setError(null)
    try {
      await enviar('/api/saldo-banco', 'POST', { fecha, importe: cantidad })
      onGuardado()
    } catch (e) {
      setError(e instanceof ErrorApi ? e.message : 'No se ha podido guardar')
    } finally {
      setGuardando(false)
    }
  }

  return (
    <form className="formulario" onSubmit={guardar}>
      <div className="campo">
        <label htmlFor="saldo-importe">Lo que tienes en el banco (€)</label>
        <input
          id="saldo-importe"
          inputMode="decimal"
          placeholder="400,00"
          value={importe}
          onChange={(e) => setImporte(e.target.value)}
          aria-describedby={error ? 'saldo-error' : undefined}
          autoFocus
        />
      </div>
      <div className="campo">
        <label htmlFor="saldo-fecha">Fecha</label>
        <input id="saldo-fecha" type="date" value={fecha} max={hoyIso()} onChange={(e) => setFecha(e.target.value)} />
      </div>
      <button className="boton boton-principal" type="submit" disabled={guardando}>
        {guardando ? 'Guardando…' : 'Guardar saldo'}
      </button>
      {onCancelar && (
        <button className="boton" type="button" onClick={onCancelar}>
          Cancelar
        </button>
      )}
      {error && (
        <p id="saldo-error" className="campo-error" role="alert">
          {error}
        </p>
      )}
    </form>
  )
}
