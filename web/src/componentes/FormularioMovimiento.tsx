import { useRef, useState, type FormEvent } from 'react'
import { enviar, ErrorApi, type Categoria, type Movimiento } from '../api'
import { avisar } from '../avisos'
import { avisarCambio, marcarNuevo } from '../cambios'
import { euros, hoyIso, importeParaEscribir, leerImporte } from '../formato'
import { useDatos } from '../useDatos'

type Tipo = 'GASTO' | 'INGRESO'

interface Props {
  editando?: Movimiento | null        // si viene, el formulario sirve para MODIFICAR ese movimiento
  onTerminarEdicion?: () => void      // al guardar los cambios, borrar o pulsar Cancelar (en la hoja: la cierra)
  enHoja?: boolean                    // dentro de la hoja que sube desde abajo: sin tarjeta y con los campos en columna
}

/** "Café −2,80 €" */
function resumen(m: Movimiento): string {
  return `${m.descripcion} ${m.tipo === 'INGRESO' ? '+' : '−'}${euros(m.importe)}`
}

/**
 * El formulario para apuntar un gasto o un ingreso. Está pensado para apuntar varios seguidos, rápido.
 * Si le pasas "editando", se rellena con ese movimiento y al guardar lo MODIFICA (PUT) en vez de crear otro.
 * (Quien lo usa le pone una "key" distinta para cada movimiento, así React lo crea de nuevo
 * y los useState de abajo arrancan con los datos del que editas.)
 * Al guardar o borrar llama a avisarCambio(): así todas las tarjetas de la pantalla se recargan solas.
 */
export function FormularioMovimiento({ editando = null, onTerminarEdicion, enHoja = false }: Props) {
  const categorias = useDatos<Categoria[]>('/api/categorias')

  const [tipo, setTipo] = useState<Tipo>(editando?.tipo ?? 'GASTO')
  const [importe, setImporte] = useState(editando ? importeParaEscribir(editando.importe) : '')
  const [descripcion, setDescripcion] = useState(editando?.descripcion ?? '')
  const [categoriaId, setCategoriaId] = useState(editando?.categoria ? String(editando.categoria.id) : '') // '' = sin categoría
  const [fecha, setFecha] = useState(editando?.fecha ?? hoyIso())

  const [error, setError] = useState<string | null>(null)
  const [hecho, setHecho] = useState<string | null>(null) // "Apuntado: Café −2,50 €"
  const [guardando, setGuardando] = useState(false)
  const [confirmandoBorrar, setConfirmandoBorrar] = useState(false)

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
        const guardado = await enviar<Movimiento>(`/api/movimientos/${editando.id}`, 'PUT', datos)
        avisarCambio()
        avisar(`Guardado: ${resumen(guardado)}`)
        onTerminarEdicion?.() // vuelve a ser el formulario de apuntar (o se cierra la hoja)
        return
      }
      const nuevo = await enviar<Movimiento>('/api/movimientos', 'POST', datos)
      marcarNuevo(`mov-${nuevo.id}`) // su fila brillará un momento en la lista
      avisarCambio()
      if (enHoja) {
        // En la hoja: se cierra y un aviso abajo dice lo que has apuntado
        avisar(`Apuntado: ${resumen(nuevo)}`)
        onTerminarEdicion?.()
        return
      }
      setHecho(`Apuntado: ${resumen(nuevo)}`)
      // Dejamos el formulario listo para el siguiente (la fecha y el tipo se quedan)
      setImporte('')
      setDescripcion('')
      setCategoriaId('')
      campoImporte.current?.focus()
    } catch (e) {
      setError(e instanceof ErrorApi ? e.message : editando ? 'No se han podido guardar los cambios' : 'No se ha podido apuntar')
    } finally {
      setGuardando(false)
    }
  }

  async function borrar() {
    if (!editando) return
    setError(null)
    try {
      await enviar(`/api/movimientos/${editando.id}`, 'DELETE')
      avisarCambio()
      avisar(`Borrado: ${resumen(editando)}`)
      onTerminarEdicion?.()
    } catch (e) {
      setError(e instanceof ErrorApi ? e.message : 'No se ha podido borrar')
    }
  }

  let textoBoton = tipo === 'GASTO' ? 'Apuntar gasto' : 'Apuntar ingreso'
  if (editando) textoBoton = 'Guardar cambios'
  if (guardando) textoBoton = editando ? 'Guardando…' : 'Apuntando…'

  const formulario = (
    <form className={enHoja ? 'formulario-hoja' : 'formulario formulario-movimiento'} onSubmit={apuntar}>
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

      <div className={enHoja ? 'campos-juntos' : 'contents'}>
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
      </div>

      <button className="boton boton-principal" type="submit" disabled={guardando}>
        {textoBoton}
      </button>

      {error && <p className="campo-error" role="alert">{error}</p>}

      {/* Borrar, solo al editar dentro de la hoja (en el ordenador ya está en la lista) */}
      {enHoja && editando && (
        <div className="hoja-borrar">
          {confirmandoBorrar ? (
            <span className="confirmar">
              ¿Borrar «{editando.descripcion}»?{' '}
              <button type="button" className="boton-texto boton-peligro" onClick={borrar}>Sí, borrar</button>
              {' · '}
              <button type="button" className="boton-texto" onClick={() => setConfirmandoBorrar(false)}>No</button>
            </span>
          ) : (
            <button type="button" className="boton-texto boton-peligro" onClick={() => setConfirmandoBorrar(true)}>
              Borrar movimiento
            </button>
          )}
        </div>
      )}
    </form>
  )

  if (enHoja) return formulario

  return (
    <section className={editando ? 'tarjeta tarjeta-editando' : 'tarjeta'} aria-labelledby="titulo-apuntar">
      <div className="tarjeta-cabecera">
        <h2 id="titulo-apuntar">{editando ? 'Editar movimiento' : 'Apuntar'}</h2>
        {hecho && !editando && <span className="etiqueta" role="status">{hecho}</span>}
        {editando && (
          <button type="button" className="boton-texto" onClick={onTerminarEdicion}>Cancelar</button>
        )}
      </div>
      {formulario}
    </section>
  )
}
