import type { PrevisionMes } from '../api'
import { euros, nombreMes } from '../formato'
import { useDatos } from '../useDatos'
import { Dinero } from './Dinero'
import { ErrorCarga } from './ErrorCarga'

/** Cómo vas a acabar el mes: lo que entra contra lo que sale (lo ya hecho y lo que falta). */
export function TarjetaMes({ version }: { version: number }) {
  const { datos, error, cargando, recargar } = useDatos<PrevisionMes>('/api/prevision', version)

  return (
    <section className="tarjeta" aria-labelledby="titulo-mes">
      <div className="tarjeta-cabecera">
        <h2 id="titulo-mes">Este mes</h2>
        {datos && <span className="etiqueta">{nombreMes(datos.mes)}</span>}
      </div>

      {cargando && !datos && (
        <div aria-busy="true" className="estado">
          <span className="cargando" style={{ width: 180, height: 32 }} />
          <span className="cargando" style={{ width: '100%', height: 10 }} />
          <span className="cargando" style={{ width: '100%', height: 120 }} />
        </div>
      )}

      {error && <ErrorCarga mensaje={error.message} onReintentar={recargar} />}

      {datos && <Contenido datos={datos} />}
    </section>
  )
}

function Contenido({ datos }: { datos: PrevisionMes }) {
  // Todo lo que entra este mes (ya cobrado + lo que falta) y todo lo que sale
  const entra = datos.ingresosApuntados + datos.ingresosPendientes
  const sale = datos.gastosApuntados + datos.gastosPendientes

  // La barra: su largo total es lo mayor de las dos cosas
  const escala = Math.max(entra, sale, 1)
  const porcentaje = (cantidad: number) => `${(cantidad / escala) * 100}%`

  return (
    <div className="mes">
      <div>
        <p className="etiqueta">Acabarás el mes con</p>
        <span className="dinero-medio">
          <Dinero cantidad={datos.saldoPrevisto} conSigno />
        </span>
      </div>

      {/* La barra: la parte gris es lo que entra; la azul, lo gastado; la clara, lo que falta por pagar */}
      <div>
        <div
          className={datos.numerosRojos ? 'barra barra-mala' : 'barra'}
          role="img"
          aria-label={`Sale ${euros(sale)} de ${euros(entra)} que entran`}
        >
          <span className="barra-entra" style={{ width: porcentaje(entra) }} />
          <span className="barra-gastado" style={{ width: porcentaje(datos.gastosApuntados) }} />
          <span
            className="barra-pendiente"
            style={{ left: porcentaje(datos.gastosApuntados), width: porcentaje(datos.gastosPendientes) }}
          />
        </div>
        <div className="barra-leyenda etiqueta num">
          <span>Sale {euros(sale)}</span>
          <span>Entra {euros(entra)}</span>
        </div>
      </div>

      <dl className="desglose num">
        <div>
          <dt>Has cobrado</dt>
          <dd className={datos.ingresosApuntados > 0 ? 'ingreso' : undefined}>+{euros(datos.ingresosApuntados)}</dd>
        </div>
        <div>
          <dt>Falta por cobrar</dt>
          <dd className={datos.ingresosPendientes > 0 ? 'ingreso' : undefined}>+{euros(datos.ingresosPendientes)}</dd>
        </div>
        <div>
          <dt>
            <span className="punto punto-gastado" aria-hidden="true" />
            Has gastado
          </dt>
          <dd>−{euros(datos.gastosApuntados)}</dd>
        </div>
        <div>
          <dt>
            <span className="punto punto-pendiente" aria-hidden="true" />
            Falta por pagar
          </dt>
          <dd>−{euros(datos.gastosPendientes)}</dd>
        </div>
      </dl>
    </div>
  )
}
