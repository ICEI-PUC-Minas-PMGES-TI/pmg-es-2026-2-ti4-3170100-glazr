import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

const root = document.getElementById('root')

if (!root) {
  throw new Error('Elemento raiz da aplicação não encontrado')
}

createRoot(root).render(
  <StrictMode>
    <main>Glazr</main>
  </StrictMode>,
)
