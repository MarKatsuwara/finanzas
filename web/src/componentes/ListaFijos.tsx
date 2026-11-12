import { useState, type CSSProperties } from 'react'
import { enviar, ErrorApi, type PagoRecurrente } from '../api'
import { avisar } from '../avisos'
import { avisarCambio, useNuevo } from '../cambios'
import { useEsMovil } from '../movil'
import { euros, fechaCorta } from '../formato'
import { nombreFrecuencia } from '../frecuencias'
import { ChipCategoria } from './ChipCategoria'
import { Interruptor } from './Interruptor'

type Filtro = 'TODOS' | 'GASTO' | 'INGRESO'

interface Props {
  fijos: PagoRecurrente[]
  onEditar: (f: PagoRecurrente) => void    // al pulsar una fila (o "Editar")
  editandoId: number | null                // el que se está editando ahora (se marca y no se puede borrar)
}

/**
 * La lista de gastos e ingresos fijos, con su interruptor.
 * Pulsar un fijo lo abre para editarlo. En el ordenador, además, tiene "Editar · Borrar";
 * en el móvil se borra desde la hoja que se abre al tocarlo.
 */
export function ListaFijos({ fijos, onEditar, editandoId }: Props) {
  const esMovil = useEsMovil()
  const nuevo = useNuevo() // el que acabas de añadir: su fila brilla un momento
  const [filtro, setFiltro] = useState<Filtro>('TODOS')
  const [confirmando, setConfirmando] = useState<number | null>(null)
  const [guardando, setGuardando] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  // Primero los activos, ordenados por la próxima fecha; al final, los desactivados
  const ordenados = [...fijos].sort((a, b) => {
    if (a.activo !== b.activo) return a.activo ? -1 : 1
    return (a.proximaFecha ?? '').localeCompare(b.proximaFecha ?? '')
  })
  const visibles = ordenados.filter((f) => filtro === 'TODOS' || f.tipo === filtro)

  /** Enciende o apaga un fijo: se envía el pago entero con "activo" cambiado. */
  async function cambiarActivo(f: PagoRecurrente) {
    setGuardando(f.id)
    setError(null)
    try {
      await enviar(`/api/pagos-recurrentes/${f.id}`, 'PUT', {
        descripcion: f.descripcion,
        tipo: f.tipo,
        importe: f.importe,
        frecuencia: f.frecuencia,
        fechaInicio: f.fechaInicio,
        categoriaId: f.categoria?.id ?? null,
        activo: !f.activo,
      })
      avisarCambio()
    } catch (e) {
      setError(e instanceof ErrorApi ? e.message : 'No se ha podido cambiar')
    } finally {
      setGuardando(null)
    }
  }

  async function borrar(f: PagoRecurrente) {
    setError(null)
    try {
      await enviar(`/api/pagos-recurrentes/${f.id}`, 'DELETE')
      setConfirmando(null)
      avisarCambio()
      avisar(`Borrado: ${f.descripcion}`)
    } catch (e) {
      setError(e instanceof ErrorApi ? e.message : 'No se ha podido borrar')
    }
  }

  const vacio =
    fijos.length === 0
      ? 'Todavía no tienes ningún fijo. Añade el primero con el formulario.'
      : filtro === 'GASTO' ? 'No tienes gastos fijos.' : 'No tienes ingresos fijos.'

  return (
    <section className="tarjeta" aria-labelledby="titulo-lista-fijos">
      <div className="tarjeta-cabecera">
        <h2 id="titulo-lista-fijos">Tus fijos</h2>
        <div className="segmentado" role="group" aria-label="Filtrar por tipo">
          <button type="button" aria-pressed={filtro === 'TODOS'} onClick={() => setFiltro('TODOS')}>Todos</button>
          <button type="button" aria-pressed={filtro === 'GASTO'} onClick={() => setFiltro('GASTO')}>Gastos</button>
          <button type="button" aria-pressed={filtro === 'INGRESO'} onClick={() => setFiltro('INGRESO')}>Ingresos</button>
        </div>
      </div>

      {error && <p className="campo-error" role="alert">{error}</p>}
      {visibles.length === 0 && <p className="vacio etiqueta">{vacio}</p>}

      <ul className="fijos">
        {visibles.map((f, i) => {
          const texto = (
            <>
              {/* Línea 1: "Alquiler · 650,00 €" (sin la categoría: así nunca salta de línea) */}
              <p className="fijo-linea">
                <span className="fijo-nombre">{f.descripcion}</span>
                <span className="separador" aria-hidden="true"> · </span>
                <span className={f.tipo === 'INGRESO' ? 'num importe ingreso' : 'num importe'}>
                  {f.tipo === 'INGRESO' ? '+' : '−'}
                  {euros(f.importe)}
                </span>
              </p>
              {/* Línea 2: categoría + "Trimestral · próximo 15 ene · 20,00 €/mes".
                  Cada trozo va entero (nowrap): si no cabe, baja el trozo completo, no media palabra */}
              <p className="fijo-detalle etiqueta">
                {f.categoria && <ChipCategoria id={f.categoria.id} nombre={f.categoria.nombre} />}
                <span className="trozo">
                  {nombreFrecuencia(f.frecuencia)}
                  {' · '}
                  {f.activo && f.proximaFecha ? `próximo ${fechaCorta(f.proximaFecha)}` : 'Desactivado'}
                </span>
                {f.activo && !f.apuntarSolo && <span className="trozo">· lo apuntas tú</span>}
                {f.frecuencia !== 'MENSUAL' && <span className="trozo num solo-escritorio">· {euros(f.importeMensual)}/mes</span>}
              </p>
            </>
          )
          const interruptor = (
            <Interruptor
              encendido={f.activo}
              onCambiar={() => cambiarActivo(f)}
              etiqueta={`${f.descripcion} activo`}
              desactivado={guardando === f.id}
            />
          )
          // "entra": la fila aparece un poquito después que la anterior (--i = su puesto)
          const clases = `fijo entra${f.activo ? '' : ' fijo-apagado'}${f.id === editandoId ? ' fijo-editando' : ''}${nuevo === `fijo-${f.id}` ? ' fila-nueva' : ''}`
          const estilo = { '--i': i } as CSSProperties

          // En el móvil: el texto es un botón que abre la hoja; el interruptor se queda a la derecha
          if (esMovil) {
            return (
              <li key={f.id} className={clases} style={estilo}>
                <button type="button" className="fijo-texto fijo-tocable" onClick={() => onEditar(f)}>
                  {texto}
                </button>
                <div className="fijo-acciones">{interruptor}</div>
              </li>
            )
          }

          // En el ordenador: pulsar la fila (fuera de los botones) también lo edita
          return (
            <li
              key={f.id}
              className={f.id === editandoId ? clases : `${clases} fijo-pulsable`}
              style={estilo}
              onClick={(e) => {
                if (!(e.target as Element).closest('button') && f.id !== editandoId) onEditar(f)
              }}
            >
              <div className="fijo-texto">{texto}</div>

              <div className="fijo-acciones">
                {interruptor}
                {f.id === editandoId ? (
                  <span className="etiqueta">Editando…</span>
                ) : confirmando === f.id ? (
                  <span className="confirmar">
                    ¿Borrar?{' '}
                    <button className="boton-texto boton-peligro" onClick={() => borrar(f)}>Sí</button>
                    {' · '}
                    <button className="boton-texto" onClick={() => setConfirmando(null)}>No</button>
                  </span>
                ) : (
                  <span className="confirmar">
                    <button className="boton-texto boton-suave" onClick={() => onEditar(f)} aria-label={`Editar ${f.descripcion}`}>
                      Editar
                    </button>
                    {' · '}
                    <button className="boton-texto boton-suave" onClick={() => setConfirmando(f.id)} aria-label={`Borrar ${f.descripcion}`}>
                      Borrar
                    </button>
                  </span>
                )}
              </div>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
