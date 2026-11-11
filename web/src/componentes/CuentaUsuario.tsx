import { useState } from 'react'
import { olvidarCuentaDeGoogle } from '../google'
import { cerrarSesion, type Usuario } from '../sesion'

/** Abajo del menú: tu foto, tu nombre, tu email y el botón de cerrar sesión. */
export function CuentaUsuario({ usuario }: { usuario: Usuario }) {
  const [fotoRota, setFotoRota] = useState(false) // si la foto no carga, ponemos tu inicial

  function salir() {
    olvidarCuentaDeGoogle()
    cerrarSesion() // la App se da cuenta y enseña la pantalla de entrar
  }

  return (
    <div className="cuenta">
      <div className="cuenta-persona">
        {usuario.foto && !fotoRota ? (
          <img
            className="cuenta-foto"
            src={usuario.foto}
            alt=""
            referrerPolicy="no-referrer" // Google a veces no enseña la foto si sabe desde qué web se pide
            onError={() => setFotoRota(true)}
          />
        ) : (
          <span className="cuenta-foto cuenta-inicial" aria-hidden="true">
            {usuario.nombre.charAt(0).toUpperCase()}
          </span>
        )}
        <div className="cuenta-datos">
          <span className="cuenta-nombre">{usuario.nombre}</span>
          <span className="cuenta-email">{usuario.email}</span>
        </div>
      </div>
      <a className="cuenta-enlace" href="#privacidad">Privacidad y cuenta</a>
      <button type="button" className="boton boton-suave cuenta-salir" onClick={salir}>
        Cerrar sesión
      </button>
    </div>
  )
}
