import { useEffect, useState } from 'react'

export type Tema = 'sistema' | 'claro' | 'oscuro'

const CLAVE = 'finanzas-tema' // el nombre con el que se guarda en el navegador

/** Lee el tema guardado. Si no hay ninguno (o el navegador no deja leer), "sistema". */
function temaGuardado(): Tema {
  try {
    const guardado = localStorage.getItem(CLAVE)
    if (guardado === 'claro' || guardado === 'oscuro') return guardado
  } catch {
    // En modo incógnito algunos navegadores no dejan usar localStorage: no pasa nada
  }
  return 'sistema'
}

/**
 * Recuerda qué tema ha elegido la persona y lo aplica a toda la página.
 * - "sistema": quita la marca y manda el tema de Windows/Mac/móvil.
 * - "claro" u "oscuro": pone data-tema="..." en <html> y el CSS hace el resto.
 */
export function useTema() {
  const [tema, setTema] = useState<Tema>(temaGuardado)

  useEffect(() => {
    const html = document.documentElement
    if (tema === 'sistema') {
      delete html.dataset.tema
    } else {
      html.dataset.tema = tema
    }

    try {
      if (tema === 'sistema') localStorage.removeItem(CLAVE)
      else localStorage.setItem(CLAVE, tema)
    } catch {
      // Si no se puede guardar, el tema funciona igual hasta que se cierre la página
    }
  }, [tema])

  return { tema, setTema }
}
