import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Todo lo que empiece por /api se reenvía a la API en Java (Spring Boot, puerto 8080).
    // Así la web y la API parecen estar "en la misma casa" y el navegador no pone pegas.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
