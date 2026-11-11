import { useCallback, useEffect, useState } from 'react'
import { ErrorApi, pedir } from './api'
import { useCambios } from './cambios'

/**
 * Un "hook" (una función especial de React) que pide datos a la API
 * y nos dice en todo momento en qué estado estamos:
 *   - cargando: todavía esperando la respuesta
 *   - error:    algo salió mal (con su mensaje)
 *   - datos:    la respuesta, cuando llega
 * Y nos da "recargar" para volver a pedirlos (por ejemplo, después de guardar algo).
 *
 * "version" es opcional: cada vez que cambia, se vuelven a pedir los datos.
 * Además, se vuelven a pedir solos cada vez que se guarda o se borra algo en cualquier
 * parte de la app (avisarCambio() de cambios.ts), así todo lo que ves está siempre al día.
 */
export function useDatos<T>(ruta: string, version = 0) {
  const cambios = useCambios()
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

  // useEffect: "cuando aparezca el componente (o cambie la ruta, o se guarde algo), pide los datos"
  useEffect(() => {
    recargar()
  }, [recargar, version, cambios])

  return { datos, error, cargando, recargar }
}

/**
 * Igual que useDatos, pero pide VARIAS rutas a la vez (por ejemplo, el resumen de 6 meses)
 * y devuelve todas las respuestas juntas, en el mismo orden.
 * Si una sola falla, se muestra el error.
 */
export function useVariosDatos<T>(rutas: string[], version = 0) {
  const cambios = useCambios()
  const [datos, setDatos] = useState<T[] | null>(null)
  const [error, setError] = useState<ErrorApi | null>(null)
  const [cargando, setCargando] = useState(true)

  // Las rutas unidas en un solo texto: así React sabe si han cambiado de verdad
  const clave = rutas.join('|')

  const recargar = useCallback(async () => {
    setCargando(true)
    setError(null)
    try {
      // Promise.all: lanza todas las peticiones a la vez y espera a que lleguen todas
      setDatos(await Promise.all(clave.split('|').map((ruta) => pedir<T>(ruta))))
    } catch (e) {
      setDatos(null)
      setError(e instanceof ErrorApi ? e : new ErrorApi(0, 'Ha ocurrido un error inesperado'))
    } finally {
      setCargando(false)
    }
  }, [clave])

  useEffect(() => {
    recargar()
  }, [recargar, version, cambios])

  return { datos, error, cargando, recargar }
}
