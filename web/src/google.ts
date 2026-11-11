// =========================================================
// El botón "Iniciar sesión con Google".
//
// Google nos da un script (Google Identity Services) que dibuja su botón oficial.
// Cuando eliges tu cuenta, nos da el "carné" (credential): un texto firmado por Google
// que la API comprobará.
// =========================================================

/** El ID de cliente de Google, de web/.env */
export const ID_CLIENTE_GOOGLE: string = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? ''

// Lo poquito que usamos del script de Google (para que TypeScript lo conozca)
interface OpcionesBoton {
  type: 'standard'
  theme: 'outline' | 'filled_black'
  size: 'large'
  text: 'signin_with'
  shape: 'pill'
  locale: string
  width: number
}
interface GoogleId {
  initialize(opciones: { client_id: string; callback: (respuesta: { credential: string }) => void }): void
  renderButton(contenedor: HTMLElement, opciones: OpcionesBoton): void
  disableAutoSelect(): void
}
declare global {
  interface Window {
    google?: { accounts: { id: GoogleId } }
  }
}

let cargando: Promise<GoogleId> | null = null

/** Carga el script de Google (solo una vez, aunque se llame varias veces). */
export function cargarGoogle(): Promise<GoogleId> {
  if (!cargando) {
    cargando = new Promise((resolver, fallar) => {
      const script = document.createElement('script')
      script.src = 'https://accounts.google.com/gsi/client'
      script.async = true
      script.onload = () => (window.google ? resolver(window.google.accounts.id) : fallar(new Error('Sin Google')))
      script.onerror = () => {
        cargando = null // así se puede volver a intentar
        fallar(new Error('No se ha podido cargar Google'))
      }
      document.head.appendChild(script)
    })
  }
  return cargando
}

/** Al cerrar sesión: que Google no vuelva a meterte solo con la misma cuenta. */
export function olvidarCuentaDeGoogle() {
  window.google?.accounts.id.disableAutoSelect()
}
