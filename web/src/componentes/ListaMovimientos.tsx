import { useState, type CSSProperties } from 'react'
import { enviar, ErrorApi, type Movimiento } from '../api'
import { avisar } from '../avisos'
import { avisarCambio, useNuevo } from '../cambios'
import { useEsMovil } from '../movil'
import { euros, nombreMes, sumar, tituloDia } from '../formato'
import { useDatos } from '../useDatos'
import { ChipCategoria } from './ChipCategoria'
import { Contador } from './Contador'
import { ErrorCarga } from './ErrorCarga'

type Filtro = 'TODOS' | 'GASTO' | 'INGRESO'

interface Props {
  mes: string                          // "2026-11": solo se enseñan los de este mes
  onEditar: (m: Movimiento) => void    // al pulsar una fila (o "Editar")
  editandoId: number | null            // el que se está editando ahora (se marca y no se puede borrar)
  direccion?: number                   // al cambiar de mes: 1 = al siguiente (la lista entra por la derecha), -1 = al anterior
}

/**
 * La lista de movimientos de un mes, agrupados por día, con filtro.
 * Pulsar una fila la abre para editarla. En el ordenador, además, cada fila tiene "Editar · Borrar";
 * en el móvil no (no caben bien): se borra desde la hoja que se abre al tocarla.
 */
export function ListaMovimientos({ mes, onEditar, editandoId, direccion = 0 }: Props) {
  const esMovil = useEsMovil()
  const nuevo = useNuevo() // el que acabas de apuntar: su fila brilla un momento
  const { datos, error, cargando, recargar } = useDatos<Movimiento[]>('/api/movimientos') // se recarga sola al guardar algo (cambios.ts)
  const [filtro, setFiltro] = useState<Filtro>('TODOS')
  const [confirmando, setConfirmando] = useState<number | null>(null) // id del que preguntamos "¿Borrar?"
  const [errorBorrar, setErrorBorrar] = useState<string | null>(null)

  // 1. Solo los de este mes (la fecha empieza por "2026-11")
  const delMes = (datos ?? []).filter((m) => m.fecha.startsWith(mes))
  // 2. Y del tipo elegido en el filtro
  const visibles = delMes.filter((m) => filtro === 'TODOS' || m.tipo === filtro)

  // Totales del mes, calculados a partir de las filas
  const entro = sumar(delMes.filter((m) => m.tipo === 'INGRESO').map((m) => m.importe))
  const salio = sumar(delMes.filter((m) => m.tipo === 'GASTO').map((m) => m.importe))

  // Agrupar por día: { "2026-11-10": [...], "2026-11-08": [...] } (la API ya los da ordenados)
  const porDia = new Map<string, Movimiento[]>()
  for (const m of visibles) {
    porDia.set(m.fecha, [...(porDia.get(m.fecha) ?? []), m])
  }

  // Animación de entrada: cada fila entra un poquito después que la anterior (--i = su puesto)
  const puesto = new Map(visibles.map((m, i) => [m.id, i]))
  const estiloFila = (m: Movimiento) => ({ '--i': puesto.get(m.id) } as CSSProperties)
  const claseFila = (m: Movimiento) => (nuevo === `mov-${m.id}` ? ' entra fila-nueva' : ' entra')

  async function borrar(m: Movimiento) {
    setErrorBorrar(null)
    try {
      await enviar(`/api/movimientos/${m.id}`, 'DELETE')
      setConfirmando(null)
      avisarCambio()
      avisar(`Borrado: ${m.descripcion}`)
    } catch (e) {
      setErrorBorrar(e instanceof ErrorApi ? e.message : 'No se ha podido borrar')
    }
  }

  const nombre = nombreMes(mes)
  const vacio = filtro === 'GASTO' ? `No hay gastos en ${nombre}.` : filtro === 'INGRESO' ? `No hay ingresos en ${nombre}.` : `No hay movimientos en ${nombre}.`

  return (
    <section className="tarjeta" aria-labelledby="titulo-lista">
      <div className="tarjeta-cabecera">
        <h2 id="titulo-lista">{nombre.charAt(0).toUpperCase() + nombre.slice(1)}</h2>
        <div className="segmentado" role="group" aria-label="Filtrar por tipo">
          <button type="button" aria-pressed={filtro === 'TODOS'} onClick={() => setFiltro('TODOS')}>Todos</button>
          <button type="button" aria-pressed={filtro === 'GASTO'} onClick={() => setFiltro('GASTO')}>Gastos</button>
          <button type="button" aria-pressed={filtro === 'INGRESO'} onClick={() => setFiltro('INGRESO')}>Ingresos</button>
        </div>
      </div>

      {cargando && !datos && (
        <div aria-busy="true" className="estado">
          {[1, 2, 3, 4].map((n) => <span key={n} className="cargando" style={{ width: '100%', height: 40 }} />)}
        </div>
      )}
      {error && <ErrorCarga mensaje={error.message} onReintentar={recargar} />}

      {datos && (
        <>
          {/* Lo que entró y salió este mes */}
          {/* (las cifras cuentan hasta su valor, también al cambiar de mes) */}
          <div className="totales-mes num">
            <div>
              <span className="etiqueta">Entró</span>
              <Contador valor={entro} formato={(n) => `+${euros(n)}`} className={entro > 0 ? 'total-cifra ingreso' : 'total-cifra'} />
            </div>
            <div>
              <span className="etiqueta">Salió</span>
              <Contador valor={salio} formato={(n) => `−${euros(n)}`} className="total-cifra" />
            </div>
          </div>

          {errorBorrar && <p className="campo-error" role="alert">{errorBorrar}</p>}

          {/* key={mes}: al cambiar de mes, todo esto se crea de nuevo y entra deslizándose hacia el lado del mes nuevo */}
          <div key={mes} className={direccion > 0 ? 'desliza-desde-derecha' : direccion < 0 ? 'desliza-desde-izquierda' : undefined}>
          {visibles.length === 0 && <p className="vacio etiqueta">{vacio}</p>}

          {[...porDia.entries()].map(([fecha, movimientos]) => (
            <div key={fecha} className="grupo-dia">
              <h3 className="grupo-titulo etiqueta">{tituloDia(fecha)}</h3>
              <ul className="movimientos">
                {movimientos.map((m) => {
                  const importe = (
                    <span className={m.tipo === 'INGRESO' ? 'num movimiento-importe ingreso' : 'num movimiento-importe'}>
                      {m.tipo === 'INGRESO' ? '+' : '−'}
                      {euros(m.importe)}
                    </span>
                  )
                  const que = (
                    <div className="movimiento-que">
                      <span className="movimiento-nombre">{m.descripcion}</span>
                      <ChipCategoria id={m.categoria?.id ?? null} nombre={m.categoria?.nombre ?? 'Sin categoría'} />
                    </div>
                  )

                  // En el móvil: toda la fila es un botón que abre la hoja de editar
                  if (esMovil) {
                    return (
                      <li key={m.id} className={`movimiento-li${claseFila(m)}`} style={estiloFila(m)}>
                        <button type="button" className="movimiento movimiento-tocable" onClick={() => onEditar(m)}>
                          {que}
                          {importe}
                        </button>
                      </li>
                    )
                  }

                  // En el ordenador: pulsar la fila (fuera de los botones) también la edita
                  return (
                    <li
                      key={m.id}
                      className={(m.id === editandoId ? 'movimiento movimiento-editando' : 'movimiento movimiento-pulsable') + claseFila(m)}
                      style={estiloFila(m)}
                      onClick={(e) => {
                        if (!(e.target as Element).closest('button') && m.id !== editandoId) onEditar(m)
                      }}
                    >
                      {que}
                      {importe}

                      <div className="movimiento-accion">
                        {m.id === editandoId ? (
                          <span className="etiqueta">Editando…</span>
                        ) : confirmando === m.id ? (
                          <span className="confirmar">
                            ¿Borrar?{' '}
                            <button className="boton-texto boton-peligro" onClick={() => borrar(m)}>Sí</button>
                            {' · '}
                            <button className="boton-texto" onClick={() => setConfirmando(null)}>No</button>
                          </span>
                        ) : (
                          <span className="confirmar">
                            <button
                              className="boton-texto boton-suave"
                              onClick={() => onEditar(m)}
                              aria-label={`Editar ${m.descripcion}`}
                            >
                              Editar
                            </button>
                            {' · '}
                            <button
                              className="boton-texto boton-suave"
                              onClick={() => setConfirmando(m.id)}
                              aria-label={`Borrar ${m.descripcion}`}
                            >
                              Borrar
                            </button>
                          </span>
                        )}
                      </div>
                    </li>
                  )
                })}
              </ul>
            </div>
          ))}
          </div>
        </>
      )}
    </section>
  )
}
