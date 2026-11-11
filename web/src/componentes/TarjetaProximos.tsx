import { useState, type CSSProperties } from 'react'
import type { ErrorApi, PrevisionBanco } from '../api'
import { diaYMes, euros, fechaCorta } from '../formato'
import { ErrorCarga } from './ErrorCarga'

interface Props {
  datos: PrevisionBanco | null
  error: ErrorApi | null
  cargando: boolean
  recargar: () => void
}

const DIAS_VISIBLES = 5 // cuántos días se ven antes de pulsar "Ver todos"

/**
 * Lo que va a entrar y salir en los próximos 30 días,
 * y cuánto dinero te quedará al final de cada uno de esos días.
 */
export function TarjetaProximos({ datos, error, cargando, recargar }: Props) {
  const [verTodos, setVerTodos] = useState(false)

  const dias = datos?.dias ?? []
  const visibles = verTodos ? dias : dias.slice(0, DIAS_VISIBLES)

  return (
    <section className="tarjeta" aria-labelledby="titulo-proximos">
      <div className="tarjeta-cabecera">
        <h2 id="titulo-proximos">Próximos 30 días</h2>
        {dias.length > DIAS_VISIBLES && (
          <button className="boton-texto" onClick={() => setVerTodos(!verTodos)}>
            {verTodos ? 'Ver menos' : `Ver los ${dias.length} días`}
          </button>
        )}
      </div>

      {cargando && !datos && (
        <div aria-busy="true" className="estado">
          {[1, 2, 3].map((n) => (
            <span key={n} className="cargando" style={{ width: '100%', height: 44 }} />
          ))}
        </div>
      )}

      {error?.estado === 404 && (
        <p className="etiqueta">Apunta tu saldo del banco y aquí verás cuánto te queda después de cada pago.</p>
      )}
      {error && error.estado !== 404 && <ErrorCarga mensaje={error.message} onReintentar={recargar} />}

      {datos && dias.length === 0 && (
        <p className="etiqueta">No tienes pagos ni cobros en los próximos 30 días.</p>
      )}

      {datos && dias.length > 0 && (
        <>
          <ol className="dias">
            {visibles.map((dia, i) => {
              const { dia: numero, mes } = diaYMes(dia.fecha)
              return (
                <li key={dia.fecha} className="dia entra" style={{ '--i': i } as CSSProperties}>
                  <div className="fecha-chip" aria-label={`${numero} de ${mes}`}>
                    <span className="fecha-dia">{numero}</span>
                    <span className="fecha-mes">{mes}</span>
                  </div>

                  <div className="dia-contenido">
                    {/* Lo que entra y sale ese día: "Netflix · −12,99 €" */}
                    <ul className="dia-movimientos">
                      {dia.movimientos.map((m, i) => (
                        <li key={i}>
                          {m.descripcion}
                          {m.origen === 'MOVIMIENTO' && <span className="etiqueta"> (apuntado)</span>}
                          <span className="separador" aria-hidden="true"> · </span>
                          <span className={m.tipo === 'INGRESO' ? 'num importe ingreso' : 'num importe'}>
                            {m.tipo === 'INGRESO' ? '+' : '−'}
                            {euros(m.importe)}
                          </span>
                        </li>
                      ))}
                    </ul>

                    {/* Y cómo queda tu dinero al acabar ese día */}
                    {dia.saldoAlFinal < 0 ? (
                      <p className="dia-saldo negativo">
                        Te quedarías en <span className="num">{euros(dia.saldoAlFinal)}</span>
                      </p>
                    ) : (
                      <p className="dia-saldo">
                        Te quedan <span className="num">{euros(dia.saldoAlFinal)}</span>
                      </p>
                    )}
                  </div>
                </li>
              )
            })}
          </ol>

          {/* Resumen: lo más bajo que llegarás y con cuánto acabas el periodo */}
          <dl className="desglose num dias-resumen">
            <div>
              <dt>Lo más bajo que llegarás</dt>
              <dd className={datos.saldoMinimo < 0 ? 'negativo' : undefined}>{euros(datos.saldoMinimo)}</dd>
            </div>
            <div className="total">
              <dt>Te quedará el {fechaCorta(datos.hasta)}</dt>
              <dd className={datos.saldoFinal < 0 ? 'negativo' : undefined}>{euros(datos.saldoFinal)}</dd>
            </div>
          </dl>
        </>
      )}
    </section>
  )
}
