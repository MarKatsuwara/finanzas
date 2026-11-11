import { useEffect, useRef, useState } from 'react'
import { entrarConGoogle, ErrorApi } from '../api'
import { cargarGoogle, ID_CLIENTE_GOOGLE } from '../google'

/** ¿La página se está viendo en oscuro? (por el selector de tema o por el sistema) */
function esTemaOscuro(): boolean {
  const elegido = document.documentElement.dataset.tema
  if (elegido) return elegido === 'oscuro'
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

/**
 * La pantalla que ves si no has entrado: el logo, una frase y el botón oficial de Google.
 * Al elegir tu cuenta, Google nos da el carné y se lo pasamos a la API (entrarConGoogle).
 */
export function Entrar() {
  const hueco = useRef<HTMLDivElement>(null) // aquí dibuja Google su botón
  const [botonListo, setBotonListo] = useState(false)
  const [entrando, setEntrando] = useState(false)
  const [error, setError] = useState<string | null>(
    ID_CLIENTE_GOOGLE ? null : 'Falta el ID de cliente de Google (VITE_GOOGLE_CLIENT_ID en web/.env).',
  )
  const [intento, setIntento] = useState(0) // sube al pulsar "Reintentar"

  useEffect(() => {
    if (!ID_CLIENTE_GOOGLE) return
    let sigueAqui = true // por si cambias de pantalla antes de que cargue

    cargarGoogle()
      .then((google) => {
        if (!sigueAqui || !hueco.current) return
        google.initialize({
          client_id: ID_CLIENTE_GOOGLE,
          // Google llama a esta función cuando eliges tu cuenta
          callback: async ({ credential }) => {
            setEntrando(true)
            setError(null)
            try {
              await entrarConGoogle(credential) // al guardar la sesión, la App enseña tu Inicio sola
            } catch (e) {
              setError(e instanceof ErrorApi ? e.message : 'No se ha podido entrar. Vuelve a intentarlo.')
              setEntrando(false)
            }
          },
        })
        hueco.current.replaceChildren() // vaciamos el hueco por si ya había un botón
        google.renderButton(hueco.current, {
          type: 'standard',
          theme: esTemaOscuro() ? 'filled_black' : 'outline',
          size: 'large',
          text: 'signin_with',
          shape: 'pill',
          locale: 'es',
          width: 280,
        })
        setBotonListo(true)
      })
      .catch(() => sigueAqui && setError('No se ha podido cargar el botón de Google. Revisa tu conexión a internet.'))

    return () => {
      sigueAqui = false
    }
  }, [intento])

  return (
    <main className="entrar">
      <section className="entrar-tarjeta" aria-labelledby="titulo-entrar">
        <svg className="entrar-logo" viewBox="0 0 32 32" aria-hidden="true">
          <rect x="5" y="20" width="22" height="6" rx="3" fill="var(--accent)" />
          <rect x="8" y="13" width="18" height="6" rx="3" fill="var(--accent)" opacity="0.7" />
          <rect x="11" y="6" width="14" height="6" rx="3" fill="var(--accent)" opacity="0.45" />
        </svg>

        <div className="entrar-texto">
          <h1 id="titulo-entrar">Controla tu dinero</h1>
          <p>Apunta lo que entra y lo que sale, y te avisamos antes de que te quedes en números rojos.</p>
        </div>

        {/* El botón de Google. Mientras carga, una barra gris del mismo tamaño */}
        <div className="entrar-boton">
          {!botonListo && !error && <span className="cargando" aria-busy="true" style={{ width: 280, height: 44 }} />}
          <div ref={hueco} hidden={entrando} />
          {entrando && <p role="status">Entrando…</p>}
        </div>

        {error && (
          <div className="estado entrar-error">
            <p className="aviso-error" role="alert">{error}</p>
            {!botonListo && ID_CLIENTE_GOOGLE && (
              <button className="boton" onClick={() => { setError(null); setIntento(intento + 1) }}>
                Reintentar
              </button>
            )}
          </div>
        )}

        <p className="etiqueta entrar-nota">
          Solo usamos tu nombre, tu email y tu foto de Google. No guardamos ninguna contraseña.{' '}
          <a href="#privacidad">Más sobre tu privacidad</a>
        </p>
      </section>
    </main>
  )
}
