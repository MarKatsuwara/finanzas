import { useAvisos } from '../avisos'

/** Los avisos de "Apuntado…", "Guardado…", abajo en el centro. Se van solos. */
export function Avisos() {
  const avisos = useAvisos()
  return (
    // role="status": el lector de pantalla lo lee en voz alta sin interrumpir
    <div className="avisos" role="status" aria-live="polite">
      {avisos.map((aviso) => (
        <p key={aviso.id} className="aviso-flotante">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
            <path d="m3.5 8.5 3 3 6-7" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
          {aviso.texto}
        </p>
      ))}
    </div>
  )
}
