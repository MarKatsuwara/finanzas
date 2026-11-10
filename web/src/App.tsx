import { useEffect, useState, type ReactNode } from 'react'
import { SelectorTema } from './componentes/SelectorTema'
import { Inicio } from './pantallas/Inicio'

// =========================================================
// Las pantallas de la app. Para añadir una nueva, se añade aquí.
// =========================================================
interface Pantalla {
  id: string            // lo que sale en la dirección: #inicio
  nombre: string        // lo que sale en el menú
  icono: ReactNode
  contenido: ReactNode
}

const PANTALLAS: Pantalla[] = [
  {
    id: 'inicio',
    nombre: 'Inicio',
    icono: (
      <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
        <path d="M2.5 7 8 2.5 13.5 7v6.5h-4v-4h-3v4h-4z" strokeLinejoin="round" />
      </svg>
    ),
    contenido: <Inicio />,
  },
]

/** Lee qué pantalla toca según la dirección (#inicio, #movimientos...). */
function pantallaDeLaDireccion(): Pantalla {
  const id = window.location.hash.replace('#', '')
  return PANTALLAS.find((p) => p.id === id) ?? PANTALLAS[0]
}

export default function App() {
  const [actual, setActual] = useState(pantallaDeLaDireccion)
  const [menuAbierto, setMenuAbierto] = useState(false)

  // Cuando cambia la dirección (al pulsar un enlace del menú), cambiamos de pantalla
  useEffect(() => {
    function alCambiar() {
      setActual(pantallaDeLaDireccion())
      setMenuAbierto(false)
    }
    window.addEventListener('hashchange', alCambiar)
    return () => window.removeEventListener('hashchange', alCambiar)
  }, [])

  return (
    <div className="app">
      <header className={menuAbierto ? 'menu abierto' : 'menu'}>
        <div className="marca">
          {/* El símbolo de la app: una pila de monedas */}
          <svg viewBox="0 0 32 32" aria-hidden="true">
            <rect x="5" y="20" width="22" height="6" rx="3" fill="var(--accent)" />
            <rect x="8" y="13" width="18" height="6" rx="3" fill="var(--accent)" opacity="0.7" />
            <rect x="11" y="6" width="14" height="6" rx="3" fill="var(--accent)" opacity="0.45" />
          </svg>
          Finanzas
        </div>

        {/* Solo se ve en el móvil */}
        <button
          className="boton-menu"
          aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú'}
          aria-expanded={menuAbierto}
          onClick={() => setMenuAbierto(!menuAbierto)}
        >
          <svg width="20" height="20" viewBox="0 0 20 20" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
            {menuAbierto ? <path d="M5 5l10 10M15 5 5 15" /> : <path d="M3 6h14M3 10h14M3 14h14" />}
          </svg>
        </button>

        {/* En el móvil, todo esto se abre con el botón del menú */}
        <div className="menu-cuerpo">
          <nav aria-label="Pantallas">
            {PANTALLAS.map((p) => (
              <a
                key={p.id}
                href={`#${p.id}`}
                className="menu-item"
                aria-current={p.id === actual.id ? 'page' : undefined}
              >
                {p.icono}
                {p.nombre}
              </a>
            ))}
          </nav>

          <SelectorTema />
        </div>
      </header>

      <main className="contenido">{actual.contenido}</main>
    </div>
  )
}
