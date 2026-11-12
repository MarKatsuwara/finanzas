import type { PagoRecurrente } from '../api'
import { euros, sumar } from '../formato'
import { FRECUENCIAS } from '../frecuencias'
import { Dinero } from './Dinero'

/**
 * Lo que te cuestan al mes todos tus gastos fijos juntos,
 * y cuánto te queda libre después de pagarlos.
 * Solo cuentan los fijos activos.
 */
export function ResumenFijos({ fijos }: { fijos: PagoRecurrente[] }) {
  const activos = fijos.filter((f) => f.activo)
  const gastosMes = sumar(activos.filter((f) => f.tipo === 'GASTO').map((f) => f.importeMensual))
  const ingresosMes = sumar(activos.filter((f) => f.tipo === 'INGRESO').map((f) => f.importeMensual))
  const libres = sumar([ingresosMes, -gastosMes])

  // Al año: cada gasto por las veces que se paga en un año (exacto, sin redondeos de por medio)
  const gastosAnio = sumar(
    activos
      .filter((f) => f.tipo === 'GASTO')
      .map((f) => f.importe * (FRECUENCIAS.find((fr) => fr.valor === f.frecuencia)?.vecesAlAnio ?? 12)),
  )

  return (
    <section className="tarjeta" aria-labelledby="titulo-resumen-fijos">
      <div className="tarjeta-cabecera">
        <h2 id="titulo-resumen-fijos">Tus gastos fijos</h2>
      </div>

      <div className="saldo">
        <div className="saldo-principal">
          <p className="cifra-con-unidad">
            <Dinero cantidad={gastosMes} grande animado />
            <span className="unidad">al mes</span>
          </p>
          <p className="etiqueta">
            <span className="num">{euros(gastosAnio)}</span> al año, repartidos entre los 12 meses.
          </p>
        </div>

        <dl className="desglose num">
          <div>
            <dt>Ingresos fijos</dt>
            <dd className={ingresosMes > 0 ? 'ingreso' : undefined}>+{euros(ingresosMes)}/mes</dd>
          </div>
          <div>
            <dt>Gastos fijos</dt>
            <dd>−{euros(gastosMes)}/mes</dd>
          </div>
          <div className="total">
            <dt>Te quedan libres</dt>
            <dd className={libres < 0 ? 'negativo' : undefined}>{euros(libres)}/mes</dd>
          </div>
        </dl>
      </div>
    </section>
  )
}
