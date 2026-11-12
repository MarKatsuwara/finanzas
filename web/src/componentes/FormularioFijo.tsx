import { useState, type FormEvent } from 'react'
import { enviar, ErrorApi, type Categoria, type Frecuencia, type PagoRecurrente } from '../api'
import { avisar } from '../avisos'
import { avisarCambio, marcarNuevo } from '../cambios'
import { euros, FECHA_MAXIMA, FECHA_MINIMA, hoyIso, importeParaEscribir, leerImporte } from '../formato'
import { aproximarAlMes, FRECUENCIAS } from '../frecuencias'
import { useDatos } from '../useDatos'
import { Interruptor } from './Interruptor'

type Tipo = 'GASTO' | 'INGRESO'

interface Props {
  editando?: PagoRecurrente | null      // si viene, el formulario sirve para MODIFICAR ese fijo
  onTerminarEdicion?: () => void        // al guardar los cambios, borrar o pulsar Cancelar (en la hoja: la cierra)
  enHoja?: boolean                      // dentro de la hoja que sube desde abajo: sin tarjeta alrededor
}

/**
 * El formulario para añadir un gasto o ingreso fijo.
 * Si le pasas "editando", se rellena con ese fijo y al guardar lo MODIFICA (PUT).
 * (Quien lo usa le pone una "key" distinta para cada fijo, igual que en Movimientos.)
 * Al guardar o borrar llama a avisarCambio(): así todas las tarjetas se recargan solas.
 */
export function FormularioFijo({ editando = null, onTerminarEdicion, enHoja = false }: Props) {
  const categorias = useDatos<Categoria[]>('/api/categorias')

  const [tipo, setTipo] = useState<Tipo>(editando?.tipo ?? 'GASTO')
  const [descripcion, setDescripcion] = useState(editando?.descripcion ?? '')
  const [importe, setImporte] = useState(editando ? importeParaEscribir(editando.importe) : '')
  const [frecuencia, setFrecuencia] = useState<Frecuencia>(editando?.frecuencia ?? 'MENSUAL')
  const [fechaInicio, setFechaInicio] = useState(editando?.fechaInicio ?? hoyIso())
  const [categoriaId, setCategoriaId] = useState(editando?.categoria ? String(editando.categoria.id) : '')
  const [apuntarSolo, setApuntarSolo] = useState(editando?.apuntarSolo ?? true)

  const [error, setError] = useState<string | null>(null)
  const [hecho, setHecho] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)
  const [confirmandoBorrar, setConfirmandoBorrar] = useState(false)

  // Mientras escribes, te dice lo que supone: al mes (o al año, si ya es mensual)
  const cantidad = leerImporte(importe)
  const hayImporte = importe.trim() !== '' && !Number.isNaN(cantidad) && cantidad > 0
  let pista: string | null = null
  if (hayImporte && frecuencia === 'MENSUAL') {
    pista = `Son ${euros(Math.round(cantidad * 12 * 100) / 100)} al año`
  } else if (hayImporte) {
    pista = `Son unos ${euros(aproximarAlMes(cantidad, frecuencia))} al mes`
  }

  async function anadir(evento: FormEvent) {
    evento.preventDefault()
    if (importe.trim() === '' || Number.isNaN(cantidad)) {
      setError('Escribe el importe, por ejemplo 60 o 12,99')
      return
    }

    setGuardando(true)
    setError(null)
    setHecho(null)
    const datos = {
      descripcion,
      tipo,
      importe: cantidad,
      frecuencia,
      fechaInicio,
      categoriaId: categoriaId === '' ? null : Number(categoriaId),
      apuntarSolo,
    }
    try {
      if (editando) {
        // Modificar: se envía todo, y "activo" se queda como estaba (eso se cambia con el interruptor)
        const guardado = await enviar<PagoRecurrente>(`/api/pagos-recurrentes/${editando.id}`, 'PUT', { ...datos, activo: editando.activo })
        avisarCambio()
        avisar(`Guardado: ${guardado.descripcion}`)
        onTerminarEdicion?.()
        return
      }
      const nuevo = await enviar<PagoRecurrente>('/api/pagos-recurrentes', 'POST', datos)
      marcarNuevo(`fijo-${nuevo.id}`) // su fila brillará un momento en la lista
      avisarCambio()
      if (enHoja) {
        avisar(`Añadido: ${nuevo.descripcion}`)
        onTerminarEdicion?.()
        return
      }
      setHecho(`Añadido: ${nuevo.descripcion}`)
      setDescripcion('')
      setImporte('')
      setCategoriaId('')
    } catch (e) {
      setError(e instanceof ErrorApi ? e.message : editando ? 'No se han podido guardar los cambios' : 'No se ha podido añadir')
    } finally {
      setGuardando(false)
    }
  }

  async function borrar() {
    if (!editando) return
    setError(null)
    try {
      await enviar(`/api/pagos-recurrentes/${editando.id}`, 'DELETE')
      avisarCambio()
      avisar(`Borrado: ${editando.descripcion}`)
      onTerminarEdicion?.()
    } catch (e) {
      setError(e instanceof ErrorApi ? e.message : 'No se ha podido borrar')
    }
  }

  let textoBoton = tipo === 'GASTO' ? 'Añadir gasto fijo' : 'Añadir ingreso fijo'
  if (editando) textoBoton = 'Guardar cambios'
  if (guardando) textoBoton = editando ? 'Guardando…' : 'Añadiendo…'

  const formulario = (
      <form className={enHoja ? 'formulario-vertical formulario-hoja' : 'formulario-vertical'} onSubmit={anadir}>
        <div className="segmentado" role="group" aria-label="Tipo">
          <button type="button" aria-pressed={tipo === 'GASTO'} onClick={() => setTipo('GASTO')}>Gasto</button>
          <button type="button" aria-pressed={tipo === 'INGRESO'} onClick={() => setTipo('INGRESO')}>Ingreso</button>
        </div>

        <div className="campo">
          <label htmlFor="fijo-descripcion">Concepto</label>
          <input
            id="fijo-descripcion"
            placeholder={tipo === 'GASTO' ? 'Alquiler, seguro, gimnasio…' : 'Nómina, beca…'}
            value={descripcion}
            maxLength={255}
            onChange={(e) => setDescripcion(e.target.value)}
            autoFocus={editando !== null || enHoja} // al abrir, el cursor salta aquí (y la página baja hasta el formulario)
          />
        </div>

        <div className="campos-juntos">
          <div className="campo">
            <label htmlFor="fijo-importe">Importe (€)</label>
            <input
              id="fijo-importe"
              inputMode="decimal"
              placeholder="0,00"
              value={importe}
              onChange={(e) => setImporte(e.target.value)}
            />
          </div>
          <div className="campo">
            <label htmlFor="fijo-frecuencia">Cada cuánto</label>
            <select
              id="fijo-frecuencia"
              value={frecuencia}
              onChange={(e) => setFrecuencia(e.target.value as Frecuencia)}
              aria-describedby="fijo-al-mes"
            >
              {FRECUENCIAS.map((f) => (
                <option key={f.valor} value={f.valor}>{f.nombre}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Lo que supone, calculado mientras escribes (solo aparece cuando hay importe) */}
        <p id="fijo-al-mes" className="al-mes etiqueta num" aria-live="polite" hidden={pista === null}>
          {pista}
        </p>

        <div className="campos-juntos">
          <div className="campo">
            <label htmlFor="fijo-fecha">Primer pago</label>
            <input id="fijo-fecha" type="date" min={FECHA_MINIMA} max={FECHA_MAXIMA} value={fechaInicio} onChange={(e) => setFechaInicio(e.target.value)} />
          </div>
          <div className="campo">
            <label htmlFor="fijo-categoria">Categoría</label>
            <select id="fijo-categoria" value={categoriaId} onChange={(e) => setCategoriaId(e.target.value)}>
              <option value="">Ninguna</option>
              {categorias.datos?.map((c) => (
                <option key={c.id} value={c.id}>{c.nombre}</option>
              ))}
            </select>
          </div>
        </div>

        {/* "Apuntar solo": cuando llega su día, la app lo apunta como movimiento */}
        <div className="opcion-interruptor">
          <div className="opcion-texto">
            <span className="opcion-titulo">Apuntar solo</span>
            <span className="etiqueta">
              Cuando llegue el día, se apunta solo en Movimientos. Apágalo si el importe cambia cada vez (luz, agua…).
            </span>
          </div>
          <Interruptor encendido={apuntarSolo} onCambiar={() => setApuntarSolo(!apuntarSolo)} etiqueta="Apuntar solo" />
        </div>

        <button className="boton boton-principal" type="submit" disabled={guardando}>
          {textoBoton}
        </button>

        {error && <p className="campo-error" role="alert">{error}</p>}
        {hecho && !error && <p className="etiqueta" role="status">{hecho}</p>}

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
                Borrar fijo
              </button>
            )}
          </div>
        )}
      </form>
  )

  if (enHoja) return formulario

  return (
    <section className={editando ? 'tarjeta tarjeta-editando' : 'tarjeta'} aria-labelledby="titulo-anadir-fijo">
      <div className="tarjeta-cabecera">
        <h2 id="titulo-anadir-fijo">{editando ? 'Editar fijo' : 'Añadir fijo'}</h2>
        {editando && (
          <button type="button" className="boton-texto" onClick={onTerminarEdicion}>Cancelar</button>
        )}
      </div>
      {formulario}
    </section>
  )
}
