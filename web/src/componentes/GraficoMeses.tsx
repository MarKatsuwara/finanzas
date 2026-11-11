import { useMemo, useState, type CSSProperties } from 'react'
import { useVisto } from '../animaciones'
import type { ResumenMes } from '../api'
import { euros, eurosRedondos, mesActual, mesCorto, moverMes, nombreMesYAnio, sumar } from '../formato'
import { barraRedondeada, escala, marcasRedondas, useAncho } from '../graficos'
import { useVariosDatos } from '../useDatos'
import { ErrorCarga } from './ErrorCarga'

const CUANTOS_MESES = 6

interface Props {
  mes: string // el mes que estás viendo abajo en la lista (se marca en el gráfico)
  onElegir: (mes: string) => void
}

/** Lo que entró y lo que salió en cada uno de los últimos 6 meses, en barras. */
export function GraficoMeses({ mes, onElegir }: Props) {
  // Los 6 meses, del más antiguo al actual: ["2026-06", ..., "2026-11"]
  const meses = useMemo(
    () => Array.from({ length: CUANTOS_MESES }, (_, n) => moverMes(mesActual(), n - (CUANTOS_MESES - 1))),
    [],
  )
  const rutas = useMemo(() => meses.map((m) => `/api/resumen?mes=${m}`), [meses])
  const { datos, error, cargando, recargar } = useVariosDatos<ResumenMes>(rutas)

  const hayAlgo = datos?.some((r) => r.totalIngresos > 0 || r.totalGastos > 0)

  return (
    <section className="tarjeta" aria-labelledby="titulo-grafico-meses">
      <div className="tarjeta-cabecera">
        <h2 id="titulo-grafico-meses">Últimos 6 meses</h2>
        {/* La leyenda: qué es cada color */}
        <div className="leyenda etiqueta" aria-hidden="true">
          <span><span className="leyenda-color leyenda-entra" />Entró</span>
          <span><span className="leyenda-color leyenda-sale" />Salió</span>
        </div>
      </div>

      {cargando && !datos && <span className="cargando" aria-busy="true" style={{ width: '100%', height: ALTO + 36 }} />}
      {error && <ErrorCarga mensaje={error.message} onReintentar={recargar} />}
      {datos && !hayAlgo && <p className="etiqueta">Cuando apuntes movimientos, aquí verás cada mes lo que entró y lo que salió.</p>}
      {datos && hayAlgo && <Grafico resumenes={datos} mes={mes} onElegir={onElegir} />}
    </section>
  )
}

const ALTO = 200
const MARGEN = { arriba: 12, abajo: 4, izquierda: 60 }

function Grafico({ resumenes, mes, onElegir }: { resumenes: ResumenMes[]; mes: string; onElegir: (mes: string) => void }) {
  const { ref, ancho } = useAncho<HTMLDivElement>()
  const { ref: refAnimacion, visto } = useVisto<HTMLDivElement>() // las barras crecen cuando el gráfico aparece en pantalla
  const [encima, setEncima] = useState<number | null>(null) // el mes que señalas

  // La regla de los euros: del 0 a la barra más alta (redondeada hacia arriba)
  const mayor = Math.max(...resumenes.flatMap((r) => [r.totalIngresos, r.totalGastos]), 1)
  const marcas = marcasRedondas(0, mayor, 4)
  const y = escala(0, marcas[marcas.length - 1], ALTO - MARGEN.abajo, MARGEN.arriba)

  // Cada mes tiene su "zona" del mismo ancho; dentro van sus dos barras, una al lado de la otra
  const anchoZona = (ancho - MARGEN.izquierda) / resumenes.length
  const anchoBarra = Math.max(6, Math.min(24, (anchoZona - 2 - 20) / 2))
  const centro = (i: number) => MARGEN.izquierda + anchoZona * i + anchoZona / 2
  const alto = (cantidad: number) => (cantidad > 0 ? Math.max(2, y(0) - y(cantidad)) : 0)

  const delMes = encima !== null ? resumenes[encima] : null

  return (
    <div ref={ref} className="grafico">
      <div ref={refAnimacion} className={visto ? 'anima visto' : 'anima'}>
      {ancho > 0 && (
        <svg width={ancho} height={ALTO} aria-hidden="true">
          {/* Las rayas horizontales con sus euros */}
          {marcas.map((marca) => (
            <g key={marca}>
              <line x1={MARGEN.izquierda} x2={ancho} y1={y(marca)} y2={y(marca)} className={marca === 0 ? 'grafico-cero' : 'grafico-rejilla'} />
              <text x={MARGEN.izquierda - 10} y={y(marca)} className="grafico-texto" textAnchor="end" dominantBaseline="middle">
                {eurosRedondos(marca)}
              </text>
            </g>
          ))}

          {resumenes.map((r, i) => {
            const apagado = encima !== null && encima !== i // los demás meses, más claritos
            return (
              <g key={r.mes} opacity={apagado ? 0.45 : 1} className="grafico-mes-zona">
                {/* El mes que estás viendo en la lista, con un fondo suave */}
                {r.mes === mes && (
                  <rect x={centro(i) - anchoZona / 2 + 4} y={0} width={anchoZona - 8} height={ALTO} rx={10} className="grafico-mes-elegido" />
                )}
                {/* Las dos barras crecen desde el suelo, mes a mes (--i: el orden, para el retraso) */}
                <path
                  d={barraRedondeada(centro(i) - 1 - anchoBarra, y(0) - alto(r.totalIngresos), anchoBarra, alto(r.totalIngresos))}
                  fill="var(--grafico-entra)"
                  className="crece"
                  style={{ '--i': i * 2 } as CSSProperties}
                />
                <path
                  d={barraRedondeada(centro(i) + 1, y(0) - alto(r.totalGastos), anchoBarra, alto(r.totalGastos))}
                  fill="var(--grafico-sale)"
                  className="crece"
                  style={{ '--i': i * 2 + 1 } as CSSProperties}
                />
                {/* Una zona invisible (más grande que las barras) para que sea fácil señalar el mes */}
                <rect
                  x={centro(i) - anchoZona / 2}
                  y={0}
                  width={anchoZona}
                  height={ALTO}
                  fill="transparent"
                  onPointerEnter={() => setEncima(i)}
                  onPointerLeave={() => setEncima(null)}
                  onClick={() => onElegir(r.mes)}
                />
              </g>
            )
          })}
        </svg>
      )}
      </div>

      {/* Los meses de abajo son botones: al pulsarlos, la lista salta a ese mes */}
      <div className="grafico-meses" style={{ paddingLeft: MARGEN.izquierda }}>
        {resumenes.map((r, i) => (
          <button
            key={r.mes}
            className="grafico-mes"
            aria-pressed={r.mes === mes}
            aria-label={`${nombreMesYAnio(r.mes)}: entró ${euros(r.totalIngresos)} y salió ${euros(r.totalGastos)}`}
            onClick={() => onElegir(r.mes)}
            onPointerEnter={() => setEncima(i)}
            onPointerLeave={() => setEncima(null)}
            onFocus={() => setEncima(i)}
            onBlur={() => setEncima(null)}
          >
            {mesCorto(r.mes)}
          </button>
        ))}
      </div>

      {/* La cajita con las cifras del mes señalado. Sale al lado del mes (no encima de sus barras) */}
      {delMes && encima !== null && (
        <div
          className="grafico-tooltip"
          data-lado={centro(encima) > ancho / 2 ? 'izquierda' : 'derecha'}
          style={{ left: centro(encima) + (centro(encima) > ancho / 2 ? -anchoZona / 2 : anchoZona / 2) }}
          aria-hidden="true"
        >
          <span className="etiqueta">{nombreMesYAnio(delMes.mes)}</span>
          <span className="grafico-tooltip-fila">
            <span><span className="leyenda-raya leyenda-entra" />Entró</span>
            <strong className="num">+{euros(delMes.totalIngresos)}</strong>
          </span>
          <span className="grafico-tooltip-fila">
            <span><span className="leyenda-raya leyenda-sale" />Salió</span>
            <strong className="num">−{euros(delMes.totalGastos)}</strong>
          </span>
          <Diferencia resumen={delMes} />
        </div>
      )}
    </div>
  )
}

/** "Te sobró 300 €" o "Gastaste 50 € más de lo que entró" */
function Diferencia({ resumen }: { resumen: ResumenMes }) {
  const diferencia = sumar([resumen.totalIngresos, -resumen.totalGastos])
  if (diferencia < 0) {
    return <span className="grafico-tooltip-total negativo">Gastaste {euros(-diferencia)} más de lo que entró</span>
  }
  return <span className="grafico-tooltip-total">Te sobró {euros(diferencia)}</span>
}
