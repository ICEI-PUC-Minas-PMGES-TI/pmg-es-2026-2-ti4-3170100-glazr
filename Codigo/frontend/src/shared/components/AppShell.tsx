import { Link, NavLink } from 'react-router-dom'
import type { ReactNode } from 'react'
import { BarChart2, Boxes, Home, Layers, LayoutDashboard, LogOut, Package, Shield, Users } from 'lucide-react'
import { useAuth } from '../../features/auth/hooks/use-auth'

function ShellLink({ to, icon, children }: Readonly<{ to: string; icon: ReactNode; children: ReactNode }>) {
  return <NavLink to={to} end={to === '/'}>
    <span className="workflow-header__icon" aria-hidden="true">{icon}</span>
    {children}
  </NavLink>
}

export function AppShell({ children }: Readonly<{ children: ReactNode }>) {
  const { logout, user } = useAuth()
  return <main className="workflow-page">
    <div className="workflow-page__aurora" aria-hidden="true" />
    <header className="workflow-header">
      <Link className="brand brand--compact" to="/dashboard">
        <img src="/glazr-fav-icon.png" alt="Glazr Logo" className="brand__logo brand__logo--compact" />
        <span>Glazr</span>
      </Link>
      <nav aria-label="Navegação principal">
        <ShellLink to="/" icon={<Home size={15} />}>Página Inicial</ShellLink>
        <ShellLink to="/dashboard" icon={<LayoutDashboard size={15} />}>Painel</ShellLink>
        {user?.permissions.includes('USER_READ') ? <ShellLink to="/admin/users" icon={<Users size={15} />}>Funcionários</ShellLink> : null}
        {user?.permissions.includes('GLASS_READ') ? <ShellLink to="/glasses" icon={<Layers size={15} />}>Vidros</ShellLink> : null}
        {user?.permissions.includes('GLASS_READ') ? <ShellLink to="/inventory" icon={<Boxes size={15} />}>Estoque</ShellLink> : null}
        {user?.permissions.includes('GLASS_READ') ? <ShellLink to="/analytics" icon={<BarChart2 size={15} />}>Indicadores</ShellLink> : null}
        {user?.permissions.includes('KIT_READ') ? <ShellLink to="/admin/kits" icon={<Package size={15} />}>Kits</ShellLink> : null}
        <ShellLink to="/account/security" icon={<Shield size={15} />}>Segurança</ShellLink>
        <button className="button button--secondary workflow-header__logout" type="button" onClick={() => void logout()}>
          <LogOut size={15} aria-hidden="true" />
          Sair
        </button>
      </nav>
    </header>
    <div className="workflow-content">{children}</div>
  </main>
}
