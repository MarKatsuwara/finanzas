import { useState } from 'react'
import type { SaldoActual } from '../api'
import { euros, fechaCorta } from '../formato'
import { useDatos } from '../useDatos'
import { Dinero } from './Dinero'
import { FormularioSaldo } from './FormularioSaldo'

/**
 * La tarjeta más importante: cuánto dinero tienes HOY en el banco.
 * Tiene 4 estados: cargando, sin saldo apuntado, error y con datos.
 */
export function TarjetaSaldo() {
  const { datos, error, cargando, recargar } = useDatos<SaldoActual>('/api/saldo-banco')
  const [editando, setEditando] = useState(false)

  function alGuardar() {
    setEditando(false)
    recargar()
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
            <span className="cargando" style={{ width: 280 }} />
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
      {error && error.estado !== 404 && (
        <div className="estado">
          <p className="aviso-error">{error.message}</p>
          <button className="boton" onClick={recargar}>Reintentar</button>
        </div>
      )}

      {/* 4. Con datos */}
      {datos && (
        <div className="saldo">
          <div className="saldo-principal">
            <Dinero cantidad={datos.saldoHoy} grande />
            <p className="etiqueta">Cuadrado con el banco el {fechaCorta(datos.saldoApuntado.fecha)}</p>

            <div className="acciones">
              {editando ? (
                <FormularioSaldo onGuardado={alGuardar} onCancelar={() => setEditando(false)} />
              ) : (
                <button className="boton" onClick={() => setEditando(true)}>
                  Cuadrar con el banco
                </button>
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
