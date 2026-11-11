import { useRef, useState, type FormEvent } from 'react'
import { enviar, ErrorApi, type Categoria, type Movimiento } from '../api'
import { euros, hoyIso, importeParaEscribir, leerImporte } from '../formato'
import { useDatos } from '../useDatos'

type Tipo = 'GASTO' | 'INGRESO'

interface Props {
  onApuntado: () => void              // avisa de que se ha guardado algo (para recargar la lista)
  editando?: Movimiento | null        // si viene, el formulario sirve para MODIFICAR ese movimiento
  onTerminarEdicion?: () => void      // al guardar los cambios o pulsar Cancelar
}

/**
 * El formulario para apuntar un gasto o un ingreso. Está pensado para apuntar varios seguidos, rápido.
 * Si le pasas "editando", se rellena con ese movimiento y al guardar lo MODIFICA (PUT) en vez de crear otro.
 * (Movimientos.tsx le pone una "key" distinta para cada movimiento, así React lo crea de nuevo
 * y los useState de abajo arrancan con los datos del que editas.)
 */
export function FormularioMovimiento({ onApuntado, editando = null, onTerminarEdicion }: Props) {
  const categorias = useDatos<Categoria[]>('/api/categorias')

  const [tipo, setTipo] = useState<Tipo>(editando?.tipo ?? 'GASTO')
  const [importe, setImporte] = useState(editando ? importeParaEscribir(editando.importe) : '')
  const [descripcion, setDescripcion] = useState(editando?.descripcion ?? '')
  const [categoriaId, setCategoriaId] = useState(editando?.categoria ? String(editando.categoria.id) : '') // '' = sin categoría
  const [fecha, setFecha] = useState(editando?.fecha ?? hoyIso())

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
    const datos = {
      tipo,
      importe: cantidad,
      fecha,
      descripcion,
      categoriaId: categoriaId === '' ? null : Number(categoriaId),
    }
    try {
      if (editando) {
        // Modificar: PUT a /api/movimientos/7 con todos los datos (también los que no has cambiado)
        await enviar<Movimiento>(`/api/movimientos/${editando.id}`, 'PUT', datos)
        onApuntado()
        onTerminarEdicion?.() // vuelve a ser el formulario de apuntar
        return
      }
      const nuevo = await enviar<Movimiento>('/api/movimientos', 'POST', datos)
      setHecho(`Apuntado: ${nuevo.descripcion} ${nuevo.tipo === 'INGRESO' ? '+' : '−'}${euros(nuevo.importe)}`)
      // Dejamos el formulario listo para el siguiente (la fecha y el tipo se quedan)
      setImporte('')
      setDescripcion('')
      setCategoriaId('')
      campoImporte.current?.focus()
      onApuntado()
    } catch (e) {
      setError(e instanceof ErrorApi ? e.message : editando ? 'No se han podido guardar los cambios' : 'No se ha podido apuntar')
    } finally {
      setGuardando(false)
    }
  }

  let textoBoton = tipo === 'GASTO' ? 'Apuntar gasto' : 'Apuntar ingreso'
  if (editando) textoBoton = 'Guardar cambios'
  if (guardando) textoBoton = editando ? 'Guardando…' : 'Apuntando…'

  return (
    <section className={editando ? 'tarjeta tarjeta-editando' : 'tarjeta'} aria-labelledby="titulo-apuntar">
      <div className="tarjeta-cabecera">
        <h2 id="titulo-apuntar">{editando ? 'Editar movimiento' : 'Apuntar'}</h2>
        {hecho && !editando && <span className="etiqueta" role="status">{hecho}</span>}
        {editando && (
          <button type="button" className="boton-texto" onClick={onTerminarEdicion}>Cancelar</button>
        )}
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
          {textoBoton}
        </button>

        {error && <p className="campo-error" role="alert">{error}</p>}
      </form>
    </section>
  )
}
