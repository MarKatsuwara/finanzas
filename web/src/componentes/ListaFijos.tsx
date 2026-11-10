import { useState } from 'react'
import { enviar, ErrorApi, type PagoRecurrente } from '../api'
import { euros, fechaCorta } from '../formato'
import { nombreFrecuencia } from '../frecuencias'
import { ChipCategoria } from './ChipCategoria'
import { Interruptor } from './Interruptor'

type Filtro = 'TODOS' | 'GASTO' | 'INGRESO'

interface Props {
  fijos: PagoRecurrente[]
  onCambio: () => void // avisa de que algo ha cambiado (activar, desactivar, borrar)
}

/** La lista de gastos e ingresos fijos, con su interruptor y su botón de borrar. */
export function ListaFijos({ fijos, onCambio }: Props) {
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
      onCambio()
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
      onCambio()
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
        {visibles.map((f) => (
          <li key={f.id} className={f.activo ? 'fijo' : 'fijo fijo-apagado'}>
            <div className="fijo-texto">
              {/* Línea 1: "Alquiler · 650,00 €" + su categoría */}
              <p className="fijo-linea">
                <span className="fijo-nombre">{f.descripcion}</span>
                <span className="separador" aria-hidden="true"> · </span>
                <span className={f.tipo === 'INGRESO' ? 'num importe ingreso' : 'num importe'}>
                  {f.tipo === 'INGRESO' ? '+' : '−'}
                  {euros(f.importe)}
                </span>
                {f.categoria && <ChipCategoria id={f.categoria.id} nombre={f.categoria.nombre} />}
              </p>
              {/* Línea 2: "Trimestral · próximo 15 ene · 20,00 €/mes" */}
              <p className="etiqueta">
                {nombreFrecuencia(f.frecuencia)}
                {' · '}
                {f.activo && f.proximaFecha ? `próximo ${fechaCorta(f.proximaFecha)}` : 'Desactivado'}
                {f.frecuencia !== 'MENSUAL' && <span className="num"> · {euros(f.importeMensual)}/mes</span>}
              </p>
            </div>

            <div className="fijo-acciones">
              <Interruptor
                encendido={f.activo}
                onCambiar={() => cambiarActivo(f)}
                etiqueta={`${f.descripcion} activo`}
                desactivado={guardando === f.id}
              />
              {confirmando === f.id ? (
                <span className="confirmar">
                  ¿Borrar?{' '}
                  <button className="boton-texto boton-peligro" onClick={() => borrar(f)}>Sí</button>
                  {' · '}
                  <button className="boton-texto" onClick={() => setConfirmando(null)}>No</button>
                </span>
              ) : (
                <button className="boton-texto boton-suave" onClick={() => setConfirmando(f.id)} aria-label={`Borrar ${f.descripcion}`}>
                  Borrar
                </button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
