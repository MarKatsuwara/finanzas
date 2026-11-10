# Finanzas · web

La web de la app de finanzas, hecha con React + TypeScript (Vite).
Pide los datos a la API en Java (Spring Boot), que tiene que estar encendida en el puerto 8080.

## Cómo arrancarla

```powershell
cd web
npm install      # solo la primera vez: descarga las librerías
npm run dev      # enciende la web en http://localhost:5173
```

Si PowerShell dice que "la ejecución de scripts está deshabilitada", usa `npm.cmd` en lugar de `npm`
(por ejemplo `npm.cmd run dev`).

## Tema claro y oscuro

Por defecto sigue el tema del sistema. En el menú, el selector **Tema** permite elegir Sistema, Claro u Oscuro
(se guarda en el navegador). Los colores de los dos temas están en `src/index.css` con `light-dark(claro, oscuro)`.

## Carpetas

- `src/pantallas/`: cada pantalla de la app (Inicio...)
- `src/componentes/`: las piezas que forman las pantallas (tarjetas, formularios...)
- `src/api.ts`: todo lo que habla con la API
- `src/formato.ts`: cómo se escriben el dinero y las fechas
- `src/useTema.ts`: recuerda y aplica el tema elegido
- `src/index.css`: los estilos; los colores están arriba del todo
