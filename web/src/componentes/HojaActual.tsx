import { cerrarHoja, useHojaAbierta } from '../hojas'
import type { Usuario } from '../sesion'
import { CuentaUsuario } from './CuentaUsuario'
import { FormularioFijo } from './FormularioFijo'
import { FormularioMovimiento } from './FormularioMovimiento'
import { Hoja } from './Hoja'
import { SelectorTema } from './SelectorTema'

/** Enseña la hoja que toque (apuntar, editar, tu cuenta…), o nada si no hay ninguna abierta. */
export function HojaActual({ usuario }: { usuario: Usuario }) {
  const hoja = useHojaAbierta()
  if (!hoja) return null

  // "key": si se abre otra hoja distinta, React la crea de nuevo (con su animación de entrada)
  if (hoja.tipo === 'movimiento') {
    return (
      <Hoja key={`mov-${hoja.editando?.id ?? 'nuevo'}`} titulo={hoja.editando ? 'Editar movimiento' : 'Apuntar'} onCerrar={cerrarHoja}>
        {(cerrar) => <FormularioMovimiento enHoja editando={hoja.editando} onTerminarEdicion={cerrar} />}
      </Hoja>
    )
  }

  if (hoja.tipo === 'fijo') {
    return (
      <Hoja key={`fijo-${hoja.editando?.id ?? 'nuevo'}`} titulo={hoja.editando ? 'Editar fijo' : 'Añadir fijo'} onCerrar={cerrarHoja}>
        {(cerrar) => <FormularioFijo enHoja editando={hoja.editando} onTerminarEdicion={cerrar} />}
      </Hoja>
    )
  }

  return (
    <Hoja key="cuenta" titulo="Tu cuenta" onCerrar={cerrarHoja}>
      {() => (
        <div className="hoja-cuenta">
          <CuentaUsuario usuario={usuario} />
          <SelectorTema />
        </div>
      )}
    </Hoja>
  )
}
