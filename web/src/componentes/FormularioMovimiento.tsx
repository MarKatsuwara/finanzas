import { useRef, useState, type FormEvent } from 'react'
import { enviar, ErrorApi, type Categoria, type Movimiento } from '../api'
import { euros, hoyIso, leerImporte } from '../formato'
import { useDatos } from '../useDatos'

type Tipo = 'GASTO' | 'INGRESO'

/** El formulario para apuntar un gasto o un ingreso. Está pensado para apuntar varios seguidos, rápido. */
export function FormularioMovimiento({ onApuntado }: { onApuntado: () => void }) {
  const categorias = useDatos<Categoria[]>('/api/categorias')

  const [tipo, setTipo] = useState<Tipo>('GASTO')
  const [importe, setImporte] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [categoriaId, setCategoriaId] = useState('') // '' = sin categoría
  const [fecha, setFecha] = useState(hoyIso())

  const [error, setError] = useState<string | null>(null)
  const [hecho, setHecho] = useState<string | null>(null) // "Apuntado: Café −2,50 €"
  const [guardando, setGuardando] = useState(false)

  // useRef: una "referencia" al campo del importe, para volver a ponerle el cursor después de apuntar
  const campoImporte = useRef<HTMLInputElement>(null)

  async function apuntar(evento: FormEvent) {
    evento.preventDefault()
    const cantidad = leerImporte(importe)
    if (importe.trim() === '' || Number.isNaN(cantidad)) {
      setError('Escribe el importe, por ejemplo 12,50')
      return
    }

    setGuardando(true)
    setError(null)
    setHecho(null)
    try {
      const nuevo = await enviar<Movimiento>('/api/movimientos', 'POST', {
        tipo,
        importe: cantidad,
        fecha,
        descripcion,
        categoriaId: categoriaId === '' ? null : Number(categoriaId),
      })
      setHecho(`Apuntado: ${nuevo.descripcion} ${nuevo.tipo === 'INGRESO' ? '+' : '−'}${euros(nuevo.importe)}`)
      // Dejamos el formulario listo para el siguiente (la fecha y el tipo se quedan)
      setImporte('')
      setDescripcion('')
      setCategoriaId('')
      campoImporte.current?.focus()
      onApuntado()
    } catch (e) {
      setError(e instanceof ErrorApi ? e.message : 'No se ha podido apuntar')
    } finally {
      setGuardando(false)
    }
  }

  return (
    <section className="tarjeta" aria-labelledby="titulo-apuntar">
      <div className="tarjeta-cabecera">
        <h2 id="titulo-apuntar">Apuntar</h2>
        {hecho && <span className="etiqueta" role="status">{hecho}</span>}
      </div>

      <form className="formulario formulario-movimiento" onSubmit={apuntar}>
        {/* Gasto o ingreso: dos botones, uno marcado */}
        <div className="campo">
          <span className="campo-titulo" id="titulo-tipo">Tipo</span>
          <div className="segmentado" role="group" aria-labelledby="titulo-tipo">
            <button type="button" aria-pressed={tipo === 'GASTO'} onClick={() => setTipo('GASTO')}>Gasto</button>
            <button type="button" aria-pressed={tipo === 'INGRESO'} onClick={() => setTipo('INGRESO')}>Ingreso</button>
          </div>
        </div>

        <div className="campo campo-importe">
          <label htmlFor="mov-importe">Importe (€)</label>
          <input
            id="mov-importe"
            ref={campoImporte}
            inputMode="decimal"
            placeholder="0,00"
            value={importe}
            onChange={(e) => setImporte(e.target.value)}
            autoFocus
          />
        </div>

        <div className="campo campo-crece">
          <label htmlFor="mov-descripcion">Concepto</label>
          <input
            id="mov-descripcion"
            placeholder={tipo === 'GASTO' ? 'Supermercado, cena…' : 'Bote, nómina…'}
            value={descripcion}
            maxLength={255}
            onChange={(e) => setDescripcion(e.target.value)}
          />
        </div>

        <div className="campo">
          <label htmlFor="mov-categoria">Categoría</label>
          <select id="mov-categoria" value={categoriaId} onChange={(e) => setCategoriaId(e.target.value)}>
            <option value="">Sin categoría</option>
            {categorias.datos?.map((c) => (
              <option key={c.id} value={c.id}>{c.nombre}</option>
            ))}
          </select>
        </div>

        <div className="campo">
          <label htmlFor="mov-fecha">Fecha</label>
          <input id="mov-fecha" type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} />
        </div>

        <button className="boton boton-principal" type="submit" disabled={guardando}>
          {guardando ? 'Apuntando…' : tipo === 'GASTO' ? 'Apuntar gasto' : 'Apuntar ingreso'}
        </button>

        {error && <p className="campo-error" role="alert">{error}</p>}
      </form>
    </section>
  )
}
