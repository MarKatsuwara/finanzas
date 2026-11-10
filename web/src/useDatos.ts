import { useCallback, useEffect, useState } from 'react'
import { ErrorApi, pedir } from './api'

/**
 * Un "hook" (una función especial de React) que pide datos a la API
 * y nos dice en todo momento en qué estado estamos:
 *   - cargando: todavía esperando la respuesta
 *   - error:    algo salió mal (con su mensaje)
 *   - datos:    la respuesta, cuando llega
 * Y nos da "recargar" para volver a pedirlos (por ejemplo, después de guardar algo).
 */
export function useDatos<T>(ruta: string) {
  const [datos, setDatos] = useState<T | null>(null)
  const [error, setError] = useState<ErrorApi | null>(null)
  const [cargando, setCargando] = useState(true)

  const recargar = useCallback(async () => {
    setCargando(true)
    setError(null)
    try {
      setDatos(await pedir<T>(ruta))
    } catch (e) {
      setDatos(null)
      setError(e instanceof ErrorApi ? e : new ErrorApi(0, 'Ha ocurrido un error inesperado'))
    } finally {
      setCargando(false)
    }
  }, [ruta])

  // useEffect: "cuando aparezca el componente (o cambie la ruta), pide los datos"
  useEffect(() => {
    recargar()
  }, [recargar])

  return { datos, error, cargando, recargar }
}
