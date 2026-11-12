import { useEffect, useState, type ReactNode } from 'react'
import { flushSync } from 'react-dom'
import { Avisos } from './componentes/Avisos'
import { AvisoDespertando } from './componentes/AvisoDespertando'
import { BarraInferior, type Destino } from './componentes/BarraInferior'
import { CuentaUsuario } from './componentes/CuentaUsuario'
import { HojaActual } from './componentes/HojaActual'
import { SelectorTema } from './componentes/SelectorTema'
import { vigilarFijosPendientes } from './fijosSolos'
import { cerrarHoja } from './hojas'
import { prefiereMenosMovimiento } from './movil'
import { Entrar } from './pantallas/Entrar'
import { GastosFijos } from './pantallas/GastosFijos'
import { Inicio } from './pantallas/Inicio'
import { Movimientos } from './pantallas/Movimientos'
import { Privacidad } from './pantallas/Privacidad'
import { alCambiarLaSesion, sesionActual } from './sesion'

// =========================================================
// Las pantallas de la app. Para añadir una nueva, se añade aquí.
// =========================================================
interface Pantalla extends Destino {
  // id: lo que sale en la dirección (#inicio); nombre: lo que sale en el menú; nombreCorto: en la barra de abajo
  contenido: ReactNode
}

const PANTALLAS: Pantalla[] = [
  {
    id: 'inicio',
    nombre: 'Inicio',
    nombreCorto: 'Inicio',
    icono: (
      <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
        <path d="M2.5 7 8 2.5 13.5 7v6.5h-4v-4h-3v4h-4z" strokeLinejoin="round" />
      </svg>
    ),
    contenido: <Inicio />,
  },
  {
    id: 'movimientos',
    nombre: 'Movimientos',
    nombreCorto: 'Movimientos',
    icono: (
      <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
        <path d="M5.5 4h8M5.5 8h8M5.5 12h8" strokeLinecap="round" />
        <circle cx="2.75" cy="4" r="0.75" fill="currentColor" stroke="none" />
        <circle cx="2.75" cy="8" r="0.75" fill="currentColor" stroke="none" />
        <circle cx="2.75" cy="12" r="0.75" fill="currentColor" stroke="none" />
      </svg>
    ),
    contenido: <Movimientos />,
  },
  {
    id: 'fijos',
    nombre: 'Gastos fijos',
    nombreCorto: 'Fijos',
    icono: (
      <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
        <path d="M2.5 7a5.5 5.5 0 0 1 9.6-3.6M13.5 9a5.5 5.5 0 0 1-9.6 3.6" strokeLinecap="round" />
        <path d="M12.5 1.5v2.5H10M3.5 14.5V12H6" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    ),
    contenido: <GastosFijos />,
  },
]

/** La página de privacidad no está en el menú, pero se puede abrir con o sin sesión (#privacidad). */
function esPrivacidad(): boolean {
  return window.location.hash === '#privacidad'
}

/** Lee qué pantalla toca según la dirección (#inicio, #movimientos...). */
function pantallaDeLaDireccion(): Pantalla {
  const id = window.location.hash.replace('#', '')
  return PANTALLAS.find((p) => p.id === id) ?? PANTALLAS[0]
}

export default function App() {
  const [actual, setActual] = useState(pantallaDeLaDireccion)
  const [enPrivacidad, setEnPrivacidad] = useState(esPrivacidad)
  const [sesion, setSesion] = useState(sesionActual) // null = no has entrado

  // Cuando entras o sales (o caduca la pulsera), volvemos a mirar la sesión
  useEffect(() => alCambiarLaSesion(() => setSesion(sesionActual())), [])

  // Con sesión: la API apunta sola los gastos fijos que ya han llegado a su día (fijosSolos.ts)
  const token = sesion?.token
  useEffect(() => (token ? vigilarFijosPendientes() : undefined), [token])

  // Cuando cambia la dirección (al pulsar un enlace del menú), cambiamos de pantalla.
  // Con "View Transitions" el navegador hace un fundido suave entre la pantalla vieja y la nueva
  // (index.css: ::view-transition-...). Si el navegador no lo sabe hacer, cambia sin más.
  useEffect(() => {
    function alCambiar() {
      const cambiar = () => {
        setActual(pantallaDeLaDireccion())
        setEnPrivacidad(esPrivacidad())
        cerrarHoja()
        window.scrollTo(0, 0) // la pantalla nueva empieza arriba
      }
      if (document.startViewTransition && !prefiereMenosMovimiento()) {
        // flushSync: que React pinte la pantalla nueva YA, dentro de la transición
        document.startViewTransition(() => flushSync(cambiar))
      } else {
        cambiar()
      }
    }
    window.addEventListener('hashchange', alCambiar)
    return () => window.removeEventListener('hashchange', alCambiar)
  }, [])

  // Sin sesión, solo se ve la pantalla de entrar (o la de privacidad, que se puede leer antes de entrar)
  if (!sesion) {
    return (
      <>
        <AvisoDespertando />
        {enPrivacidad ? (
          <main className="contenido contenido-suelto">
            <a className="boton-texto volver" href="#">← Volver</a>
            <Privacidad conSesion={false} />
          </main>
        ) : (
          <Entrar />
        )}
      </>
    )
  }

  return (
    <div className="app">
      <AvisoDespertando />
      <header className="menu">
        <div className="marca">
          {/* El símbolo de la app: una pila de monedas */}
          <svg viewBox="0 0 32 32" aria-hidden="true">
            <rect x="5" y="20" width="22" height="6" rx="3" fill="var(--accent)" />
            <rect x="8" y="13" width="18" height="6" rx="3" fill="var(--accent)" opacity="0.7" />
            <rect x="11" y="6" width="14" height="6" rx="3" fill="var(--accent)" opacity="0.45" />
          </svg>
          Finanzas
        </div>

        {/* En el móvil, esto no se ve: lo sustituye la barra de abajo (BarraInferior) */}
        <div className="menu-cuerpo">
          <nav aria-label="Pantallas">
            {PANTALLAS.map((p) => (
              <a
                key={p.id}
                href={`#${p.id}`}
                className="menu-item"
                aria-current={p.id === actual.id && !enPrivacidad ? 'page' : undefined}
              >
                {p.icono}
                {p.nombre}
              </a>
            ))}
          </nav>

          <SelectorTema />
          <CuentaUsuario usuario={sesion.usuario} />
        </div>
      </header>

      <main className="contenido">{enPrivacidad ? <Privacidad conSesion /> : actual.contenido}</main>

      {/* Solo en el móvil (index.css la esconde en el ordenador) */}
      <BarraInferior
        destinos={PANTALLAS}
        actual={enPrivacidad ? null : actual.id}
        inicial={sesion.usuario.nombre.charAt(0).toUpperCase()}
      />
      <HojaActual usuario={sesion.usuario} />
      <Avisos />
    </div>
  )
}
