import { useTema, type Tema } from '../useTema'

const OPCIONES: { valor: Tema; nombre: string }[] = [
  { valor: 'sistema', nombre: 'Sistema' },
  { valor: 'claro', nombre: 'Claro' },
  { valor: 'oscuro', nombre: 'Oscuro' },
]

/** Tres botones para elegir el tema: Sistema · Claro · Oscuro. */
export function SelectorTema() {
  const { tema, setTema } = useTema()

  return (
    <div className="tema">
      <span className="etiqueta" id="titulo-tema">Tema</span>
      <div className="tema-opciones" role="group" aria-labelledby="titulo-tema">
        {OPCIONES.map((opcion) => (
          <button
            key={opcion.valor}
            type="button"
            aria-pressed={tema === opcion.valor} // le dice al lector de pantalla cuál está elegido
            onClick={() => setTema(opcion.valor)}
          >
            {opcion.nombre}
          </button>
        ))}
      </div>
    </div>
  )
}
