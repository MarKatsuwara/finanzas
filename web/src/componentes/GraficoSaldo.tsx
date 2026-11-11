import { useId, useState, type CSSProperties, type KeyboardEvent, type PointerEvent } from 'react'
import { useVisto } from '../animaciones'
import type { DiaPrevisto, ErrorApi, PrevisionBanco } from '../api'
import { diasEntre, euros, eurosRedondos, fechaCorta, sumarDias } from '../formato'
import { escala, marcasRedondas, useAncho } from '../graficos'
import { ErrorCarga } from './ErrorCarga'

interface Props {
  datos: PrevisionBanco | null
  error: ErrorApi | null
  cargando: boolean
  recargar: () => void
}

/** La tarjeta del gráfico: "Cómo irá tu saldo" en los próximos 30 días, con sus estados. */
export function GraficoSaldo({ datos, error, cargando, recargar }: Props) {
  return (
    <section className="tarjeta" aria-labelledby="titulo-grafico-saldo">
      <div className="tarjeta-cabecera">
        <h2 id="titulo-grafico-saldo">Cómo irá tu saldo</h2>
        {datos && <span className="etiqueta">Día a día, hasta el {fechaCorta(datos.hasta)}</span>}
      </div>

      {cargando && !datos && <span className="cargando" aria-busy="true" style={{ width: '100%', height: ALTO }} />}

      {error?.estado === 404 && (
        <p className="etiqueta">Apunta tu saldo del banco y aquí verás cómo irá tu dinero día a día.</p>
      )}
      {error && error.estado !== 404 && <ErrorCarga mensaje={error.message} onReintentar={recargar} />}

      {datos && <Grafico datos={datos} />}
    </section>
  )
}

const ALTO = 220
// El hueco que dejamos alrededor del dibujo para los textos de los ejes
const MARGEN = { arriba: 24, derecha: 8, abajo: 28, izquierda: 60 }

/** El gráfico de verdad: una línea con el saldo al final de cada día. */
function Grafico({ datos }: { datos: PrevisionBanco }) {
  const { ref, ancho } = useAncho<HTMLDivElement>()
  const { ref: refAnimacion, visto } = useVisto<HTMLDivElement>() // la línea se dibuja cuando el gráfico aparece en pantalla
  const [elegido, setElegido] = useState<number | null>(null) // el día que señalas con el ratón o el dedo
  const id = useId() // un nombre único para los "recortes" del SVG

  // 1. El saldo al final de CADA día (día 0 = hoy).
  //    La API solo nos manda los días en los que pasa algo; los demás días el saldo no cambia.
  const totalDias = Math.max(diasEntre(datos.hoy, datos.hasta), 1)
  const diasConMovimientos = new Map<number, DiaPrevisto>()
  for (const dia of datos.dias) diasConMovimientos.set(diasEntre(datos.hoy, dia.fecha), dia)

  const saldos: number[] = []
  let saldo = datos.saldoHoy
  for (let i = 0; i <= totalDias; i++) {
    const dia = diasConMovimientos.get(i)
    if (dia) saldo = dia.saldoAlFinal
    saldos.push(saldo)
  }

  // 2. Las "reglas": de día a píxel horizontal, y de euros a píxel vertical.
  //    El 0 siempre se ve, para saber lo lejos (o cerca) que estás de los números rojos.
  const marcas = marcasRedondas(Math.min(0, ...saldos), Math.max(0, ...saldos), 4)
  const x = escala(0, totalDias, MARGEN.izquierda, ancho - MARGEN.derecha)
  const y = escala(marcas[0], marcas[marcas.length - 1], ALTO - MARGEN.abajo, MARGEN.arriba)
  const yCero = y(0)

  // 3. La línea, en "escalones": el saldo se queda igual hasta el día que entra o sale dinero.
  //    M = empieza aquí, H = sigue en horizontal hasta, V = sube o baja hasta.
  let linea = `M${x(0)},${y(saldos[0])}`
  for (let i = 1; i <= totalDias; i++) {
    if (saldos[i] !== saldos[i - 1]) linea += ` H${x(i)} V${y(saldos[i])}`
  }
  linea += ` H${x(totalDias)}`
  const area = `${linea} V${yCero} H${x(0)} Z` // lo mismo, pero cerrado hasta el 0 para rellenarlo

  // 4. Las fechas de abajo: hoy, una cada semana (cada 2 en el móvil) y la última
  const cada = ancho < 480 ? 14 : 7
  const fechasEje = [0]
  for (let i = cada; i <= totalDias - cada / 2; i += cada) fechasEje.push(i)
  fechasEje.push(totalDias)

  const nombreDia = (i: number) => (i === 0 ? 'Hoy' : fechaCorta(sumarDias(datos.hoy, i)))

  // 5. Señalar un día: con el ratón, con el dedo o con las flechas del teclado
  function alMover(evento: PointerEvent<SVGSVGElement>) {
    const caja = evento.currentTarget.getBoundingClientRect()
    const anchoDibujo = ancho - MARGEN.izquierda - MARGEN.derecha
    const dia = Math.round(((evento.clientX - caja.left - MARGEN.izquierda) / anchoDibujo) * totalDias)
    setElegido(Math.min(totalDias, Math.max(0, dia)))
  }
  function alPulsarTecla(evento: KeyboardEvent<SVGSVGElement>) {
    const actual = elegido ?? 0
    const nuevo: Record<string, number> = {
      ArrowRight: Math.min(totalDias, actual + 1),
      ArrowLeft: Math.max(0, actual - 1),
      Home: 0,
      End: totalDias,
    }
    if (evento.key in nuevo) {
      evento.preventDefault()
      setElegido(nuevo[evento.key])
    }
  }

  const ultimo = saldos[totalDias]
  const color = (cantidad: number) => (cantidad < 0 ? 'var(--negative)' : 'var(--accent)')

  return (
    <div ref={ref} className="grafico">
      <div ref={refAnimacion} className={visto ? 'anima visto' : 'anima'}>
      {ancho > 0 && (
        <svg
          width={ancho}
          height={ALTO}
          role="img"
          tabIndex={0}
          aria-label={`Tu saldo los próximos ${totalDias} días: hoy ${euros(datos.saldoHoy)}, lo más bajo ${euros(
            datos.saldoMinimo,
          )} y el ${fechaCorta(datos.hasta)} ${euros(ultimo)}. Usa las flechas para ver cada día.`}
          onPointerMove={alMover}
          onPointerDown={alMover}
          onPointerLeave={(e) => e.pointerType === 'mouse' && setElegido(null)}
          onKeyDown={alPulsarTecla}
          onFocus={() => setElegido(elegido ?? 0)}
          onBlur={() => setElegido(null)}
        >
          {/* Dos "recortes": lo que queda por encima del 0 y lo que queda por debajo */}
          <defs>
            <clipPath id={`${id}-arriba`}>
              <rect x={0} y={0} width={ancho} height={Math.max(0, yCero)} />
            </clipPath>
            <clipPath id={`${id}-abajo`}>
              <rect x={0} y={yCero} width={ancho} height={Math.max(0, ALTO - yCero)} />
            </clipPath>
          </defs>

          {/* Las rayas horizontales con sus euros (la del 0, un poco más marcada) */}
          {marcas.map((marca) => (
            <g key={marca}>
              <line
                x1={MARGEN.izquierda}
                x2={ancho - MARGEN.derecha}
                y1={y(marca)}
                y2={y(marca)}
                className={marca === 0 ? 'grafico-cero' : 'grafico-rejilla'}
              />
              <text x={MARGEN.izquierda - 10} y={y(marca)} className="grafico-texto" textAnchor="end" dominantBaseline="middle">
                {eurosRedondos(marca)}
              </text>
            </g>
          ))}

          {/* Las fechas de abajo */}
          {fechasEje.map((i, n) => (
            <text
              key={i}
              x={x(i)}
              y={ALTO - 6}
              className="grafico-texto"
              textAnchor={n === 0 ? 'start' : n === fechasEje.length - 1 ? 'end' : 'middle'}
            >
              {nombreDia(i)}
            </text>
          ))}

          {/* El relleno suave y la línea: azul por encima del 0, rojo por debajo.
              pathLength={1}: la línea "mide 1", así index.css puede dibujarla de 0 a 1 (.anima .dibuja) */}
          <path d={area} fill="var(--accent)" opacity={0.1} clipPath={`url(#${id}-arriba)`} className="aparece-tarde" />
          <path d={area} fill="var(--negative)" opacity={0.12} clipPath={`url(#${id}-abajo)`} className="aparece-tarde" />
          <path d={linea} pathLength={1} className="grafico-linea dibuja" stroke="var(--accent)" clipPath={`url(#${id}-arriba)`} />
          <path d={linea} pathLength={1} className="grafico-linea dibuja" stroke="var(--negative)" clipPath={`url(#${id}-abajo)`} />

          {/* Un puntito en cada día que entra o sale dinero. Salen en orden, según la línea va llegando
              (--llega: en qué momento de la animación pasa la línea por ese día) */}
          {[...diasConMovimientos.keys()].map((i) => (
            <circle
              key={i}
              cx={x(i)}
              cy={y(saldos[i])}
              r={4}
              fill={color(saldos[i])}
              className="grafico-punto salta"
              style={{ '--llega': `${Math.round((i / totalDias) * 900)}ms` } as CSSProperties}
            />
          ))}

          {/* El saldo del último día, escrito al final de la línea */}
          {elegido === null && (
            <text x={x(totalDias)} y={y(ultimo) - 10} textAnchor="end" className="grafico-texto grafico-texto-fuerte aparece-tarde">
              {euros(ultimo)}
            </text>
          )}

          {/* La raya vertical que sigue al ratón, con su puntito */}
          {elegido !== null && (
            <g aria-hidden="true">
              <line x1={x(elegido)} x2={x(elegido)} y1={MARGEN.arriba - 8} y2={ALTO - MARGEN.abajo} className="grafico-guia" />
              <circle cx={x(elegido)} cy={y(saldos[elegido])} r={5} fill={color(saldos[elegido])} className="grafico-punto" />
            </g>
          )}
        </svg>
      )}
      </div>

      {/* La cajita con los datos del día señalado (también la leen los lectores de pantalla) */}
      <div aria-live="polite">
        {elegido !== null && (
          <div
            className="grafico-tooltip"
            data-lado={x(elegido) > ancho / 2 ? 'izquierda' : 'derecha'}
            style={{ left: x(elegido) }}
          >
            <strong className={saldos[elegido] < 0 ? 'num negativo' : 'num'}>{euros(saldos[elegido])}</strong>
            <span className="etiqueta">
              {saldos[elegido] < 0 ? 'Te quedarías en números rojos' : 'Te quedan'} · {nombreDia(elegido)}
            </span>
            {diasConMovimientos.get(elegido)?.movimientos.map((m, n) => (
              <span key={n} className="grafico-tooltip-fila">
                {m.descripcion}
                <span className="num">
                  {m.tipo === 'INGRESO' ? '+' : '−'}
                  {euros(m.importe)}
                </span>
              </span>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
