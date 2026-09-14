import { Link } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../../features/auth/hooks/use-auth'

export function AppShell({ children }: Readonly<{ children: ReactNode }>) {
  const { logout, user } = useAuth()
  return <main className="workflow-page">
    <header className="workflow-header">
      <Link className="brand brand--compact" to="/dashboard">
        <img src="/glazr-fav-icon.png" alt="Glazr Logo" className="brand__logo brand__logo--compact" />
        <span>Glazr</span>
      </Link>
      <nav aria-label="Navegação principal">
        <Link to="/">Página Inicial</Link>
        <Link to="/dashboard">Painel</Link>
        {user?.permissions.includes('USER_READ') ? <Link to="/admin/users">Funcionários</Link> : null}
        {user?.permissions.includes('GLASS_READ') ? <Link to="/glasses">Vidros</Link> : null}
        {user?.permissions.includes('KIT_READ') ? <Link to="/admin/kits">Kits</Link> : null}
        <Link to="/account/security">Segurança</Link>
        <button className="button button--secondary" type="button" onClick={() => void logout()}>Sair</button>
      </nav>
    </header>
    <div className="workflow-content">{children}</div>
  </main>
}
