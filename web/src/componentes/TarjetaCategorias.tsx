import type { ResumenMes, TotalCategoria } from '../api'
import { euros, nombreMes } from '../formato'
import { useDatos } from '../useDatos'
import { ChipCategoria } from './ChipCategoria'
import { ErrorCarga } from './ErrorCarga'

/** En qué se te ha ido el dinero este mes y de dónde ha venido, por categorías. */
export function TarjetaCategorias({ mes, version }: { mes: string; version: number }) {
  const { datos, error, cargando, recargar } = useDatos<ResumenMes>(`/api/resumen?mes=${mes}`, version)

  return (
    <section className="tarjeta" aria-labelledby="titulo-categorias">
      <div className="tarjeta-cabecera">
        <h2 id="titulo-categorias">Por categoría</h2>
      </div>

      {cargando && !datos && (
        <div aria-busy="true" className="estado">
          {[1, 2, 3].map((n) => <span key={n} className="cargando" style={{ width: '100%', height: 36 }} />)}
        </div>
      )}
      {error && <ErrorCarga mensaje={error.message} onReintentar={recargar} />}

      {datos && (
        <div className="por-categoria">
          <Ranking
            titulo="Gastos"
            total={datos.totalGastos}
            filas={datos.gastosPorCategoria}
            vacio={`Sin gastos en ${nombreMes(mes)}.`}
            tipo="gasto"
          />
          <Ranking
            titulo="Ingresos"
            total={datos.totalIngresos}
            filas={datos.ingresosPorCategoria}
            vacio={`Sin ingresos en ${nombreMes(mes)}.`}
            tipo="ingreso"
          />
        </div>
      )}
    </section>
  )
}

interface RankingProps {
  titulo: string
  total: number
  filas: TotalCategoria[]
  vacio: string
  tipo: 'gasto' | 'ingreso'
}

/** Una lista de categorías de mayor a menor, cada una con su barrita proporcional. */
function Ranking({ titulo, total, filas, vacio, tipo }: RankingProps) {
  const mayor = Math.max(...filas.map((f) => f.total), 1) // la categoría más grande ocupa toda la barra

  return (
    <div>
      <div className="ranking-cabecera">
        <h3>{titulo}</h3>
        <span className={tipo === 'ingreso' && total > 0 ? 'num ingreso' : 'num'}>
          {tipo === 'ingreso' ? '+' : '−'}
          {euros(total)}
        </span>
      </div>

      {filas.length === 0 ? (
        <p className="etiqueta">{vacio}</p>
      ) : (
        <ul className="ranking">
          {filas.map((f) => (
            <li key={f.categoriaId ?? 'sin'}>
              <div className="ranking-fila">
                <ChipCategoria id={f.categoriaId} nombre={f.categoria} />
                <span className="num">{euros(f.total)}</span>
              </div>
              <span
                className={tipo === 'ingreso' ? 'ranking-barra ranking-barra-ingreso' : 'ranking-barra'}
                style={{ width: `${(f.total / mayor) * 100}%` }}
              />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
