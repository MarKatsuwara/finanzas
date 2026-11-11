// Pone el tema elegido ANTES de pintar la página, para que no se vea un "flash" del otro tema.
// Está en un archivo aparte (y no dentro de index.html) porque la regla de seguridad CSP
// de netlify.toml solo deja ejecutar código que venga en archivos de la propia web.
try {
  var tema = localStorage.getItem('finanzas-tema')
  if (tema === 'claro' || tema === 'oscuro') document.documentElement.dataset.tema = tema
} catch (e) {}
