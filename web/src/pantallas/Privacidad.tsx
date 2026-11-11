import { useState } from 'react'
import { borrarMiCuenta, ErrorApi } from '../api'
import { olvidarCuentaDeGoogle } from '../google'

/**
 * Qué datos guarda la app y para qué (lo pide la ley de protección de datos, el RGPD).
 * Si has entrado, abajo puedes borrar tu cuenta.
 */
export function Privacidad({ conSesion }: { conSesion: boolean }) {
  return (
    <>
      <div className="titulo">
        <div>
          <h1>Privacidad</h1>
          <p>Qué guardamos, para qué y cómo borrarlo</p>
        </div>
      </div>

      <div className="inicio privacidad">
        <section className="tarjeta texto-legal" aria-label="Política de privacidad">
          <h2>Qué datos guardamos</h2>
          <ul>
            <li><strong>De tu cuenta de Google:</strong> tu nombre, tu email y tu foto. Nada más: no vemos tu contraseña ni tu correo.</li>
            <li><strong>Lo que tú apuntas:</strong> movimientos, gastos fijos, saldos del banco y categorías.</li>
          </ul>

          <h2>Para qué</h2>
          <p>
            Solo para que la app funcione: enseñarte tus cuentas, tus avisos y tus gráficos.
            No vendemos ni compartimos tus datos con nadie, no hay publicidad y nadie más puede verlos.
          </p>

          <h2>Dónde se guardan</h2>
          <p>
            En los servidores que usa la app para funcionar: la base de datos en <strong>Neon</strong>,
            la API en <strong>Render</strong> y la web en <strong>Netlify</strong>.
            Para entrar usamos <strong>Google</strong>, que tiene su propia política de privacidad.
          </p>

          <h2>Cookies y almacenamiento</h2>
          <p>
            No usamos cookies de publicidad ni de seguimiento. Tu navegador guarda tu sesión (para que no tengas
            que entrar cada vez) y el tema que elijas. El botón de Google puede usar sus propias cookies para iniciar sesión.
          </p>

          <h2>Cuánto tiempo</h2>
          <p>Mientras tengas la cuenta. Si la borras, se borra todo al momento y no se puede recuperar.</p>

          <h2>Quién está detrás</h2>
          <p>
            Es un proyecto personal de portfolio. Si tienes cualquier duda, puedes escribir en{' '}
            <a href="https://github.com/MarKatsuwara/finanzas/issues" target="_blank" rel="noreferrer">
              el repositorio del proyecto en GitHub
            </a>.
          </p>

          <p className="etiqueta">Última actualización: 11 de noviembre de 2026.</p>
        </section>

        {conSesion && <BorrarCuenta />}
      </div>
    </>
  )
}

/** Borrar la cuenta, con un paso de confirmación (porque no se puede deshacer). */
function BorrarCuenta() {
  const [confirmando, setConfirmando] = useState(false)
  const [borrando, setBorrando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function borrar() {
    setBorrando(true)
    setError(null)
    try {
      await borrarMiCuenta() // al cerrarse la sesión, la App enseña la pantalla de entrar
      olvidarCuentaDeGoogle()
    } catch (e) {
      setError(e instanceof ErrorApi ? e.message : 'No se ha podido borrar la cuenta')
      setBorrando(false)
    }
  }

  return (
    <section className="tarjeta" aria-labelledby="titulo-borrar-cuenta">
      <div className="tarjeta-cabecera">
        <h2 id="titulo-borrar-cuenta">Borrar mi cuenta</h2>
      </div>

      <div className="estado">
        <p>
          Se borrarán tu cuenta y <strong>todo lo que has apuntado</strong>: movimientos, gastos fijos, saldos y
          categorías. No se puede deshacer.
        </p>

        {!confirmando ? (
          <button className="boton boton-peligro" onClick={() => setConfirmando(true)}>
            Borrar mi cuenta
          </button>
        ) : (
          <div className="confirmar-borrado" role="group" aria-label="Confirmar el borrado">
            <p><strong>¿Seguro? Esto borra todo para siempre.</strong></p>
            <div className="acciones">
              <button className="boton boton-borrar-todo" onClick={borrar} disabled={borrando}>
                {borrando ? 'Borrando…' : 'Sí, borrar todo'}
              </button>
              <button className="boton" onClick={() => setConfirmando(false)} disabled={borrando}>
                Cancelar
              </button>
            </div>
          </div>
        )}

        {error && <p className="campo-error" role="alert">{error}</p>}
      </div>
    </section>
  )
}
