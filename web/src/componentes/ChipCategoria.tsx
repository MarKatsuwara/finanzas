/**
 * La etiqueta de color de una categoría: [Comida], [Propinas]...
 * Cada categoría tiene siempre el mismo color pastel, elegido a partir de su id
 * (hay 6 colores; la categoría 7 vuelve a usar el color 1, y así).
 */
export function ChipCategoria({ id, nombre }: { id: number | null; nombre: string }) {
  const tinte = id === null ? 'tinte-neutro' : `tinte-${((id - 1) % 6) + 1}`
  return <span className={`chip ${tinte}`}>{nombre}</span>
}
