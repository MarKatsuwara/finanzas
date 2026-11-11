import { useState } from 'react'
import type { PrevisionBanco, SaldoActual } from '../api'
import { euros, fechaCorta } from '../formato'
import { useDatos } from '../useDatos'
import { Dinero } from './Dinero'
import { ErrorCarga } from './ErrorCarga'
import { FormularioSaldo } from './FormularioSaldo'

interface Props {
  version: number                   // cuando cambia, se vuelve a pedir el saldo
  prevision: PrevisionBanco | null  // para el aviso: ¿me voy a quedar en negativo?
  onCambio: () => void              // avisa a la pantalla de que el saldo ha cambiado
}

/**
 * La tarjeta más importante: cuánto dinero tienes HOY en el banco
 * y si te vas a quedar en negativo en los próximos días.
 */
export function TarjetaSaldo({ version, prevision, onCambio }: Props) {
  const { datos, error, cargando, recargar } = useDatos<SaldoActual>('/api/saldo-banco', version)
  const [editando, setEditando] = useState(false)

  function alGuardar() {
    setEditando(false)
    onCambio() // así se actualizan también las otras tarjetas
  }

  return (
    <section className="tarjeta" aria-labelledby="titulo-saldo">
      <div className="tarjeta-cabecera">
        <h2 id="titulo-saldo">En el banco hoy</h2>
      </div>

      {/* 1. Cargando: barras grises del tamaño del dato */}
      {cargando && !datos && (
        <div className="saldo" aria-busy="true">
          <div className="saldo-principal">
            <span className="cargando" style={{ width: 220, height: 40 }} />
            <span className="cargando" style={{ width: 300, height: 36 }} />
          </div>
          <span className="cargando" style={{ height: 150, borderRadius: 12 }} />
        </div>
      )}

      {/* 2. Nunca has apuntado tu saldo (la API responde 404) */}
      {error?.estado === 404 && (
        <div className="estado">
          <p>Apunta cuánto dinero tienes ahora en el banco. A partir de ahí la app irá sumando y restando lo que apuntes.</p>
          <FormularioSaldo onGuardado={alGuardar} />
        </div>
      )}

      {/* 3. Otro error (por ejemplo, la API está apagada) */}
      {error && error.estado !== 404 && <ErrorCarga mensaje={error.message} onReintentar={recargar} />}

      {/* 4. Con datos */}
      {datos && (
        <div className="saldo">
          <div className="saldo-principal">
            <Dinero cantidad={datos.saldoHoy} grande animado />
            {prevision && <Aviso prevision={prevision} />}

            <div className="acciones">
              {editando ? (
                <FormularioSaldo onGuardado={alGuardar} onCancelar={() => setEditando(false)} />
              ) : (
                <p className="etiqueta">
                  Cuadrado con el banco el {fechaCorta(datos.saldoApuntado.fecha)} ·{' '}
                  <button className="boton-texto" onClick={() => setEditando(true)}>
                    Cuadrar ahora
                  </button>
                </p>
              )}
            </div>
          </div>

          {/* Cómo se ha calculado: una pequeña "suma" */}
          <dl className="desglose num">
            <div>
              <dt>Apuntaste el {fechaCorta(datos.saldoApuntado.fecha)}</dt>
              <dd>{euros(datos.saldoApuntado.importe)}</dd>
            </div>
            <div>
              <dt>Ingresos desde entonces</dt>
              {/* Verde solo si de verdad ha entrado dinero; un cero va en gris */}
              <dd className={datos.ingresosDesde > 0 ? 'ingreso' : undefined}>+{euros(datos.ingresosDesde)}</dd>
            </div>
            <div>
              <dt>Gastos desde entonces</dt>
              <dd>−{euros(datos.gastosDesde)}</dd>
            </div>
            <div className="total">
              <dt>Hoy</dt>
              <dd>{euros(datos.saldoHoy)}</dd>
            </div>
          </dl>
        </div>
      )}
    </section>
  )
}

/** La frase que responde a "¿me voy a quedar sin dinero?", en verde o en rojo. */
function Aviso({ prevision }: { prevision: PrevisionBanco }) {
  const dias = 30
  let texto: string

  if (!prevision.numerosRojos) {
    texto = `En los próximos ${dias} días no bajarás de ${euros(prevision.saldoMinimo)}`
  } else if (prevision.primerDiaEnNegativo === prevision.hoy) {
    texto = `Ya estás en negativo: ${euros(prevision.saldoHoy)}`
  } else {
    // Buscamos cómo queda el saldo el primer día que baja de cero
    const dia = prevision.dias.find((d) => d.fecha === prevision.primerDiaEnNegativo)
    texto = `El ${fechaCorta(prevision.primerDiaEnNegativo!)} te quedarás en ${euros(dia?.saldoAlFinal ?? prevision.saldoMinimo)}`
  }

  return (
    <p className={prevision.numerosRojos ? 'aviso aviso-malo' : 'aviso aviso-bueno'} role="status">
      {texto}
    </p>
  )
}
