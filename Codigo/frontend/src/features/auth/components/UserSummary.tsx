import { Link } from 'react-router-dom'
import { ArrowRight, Package, Shield, Users } from 'lucide-react'
import { useAuth } from '../hooks/use-auth'
import './UserSummary.css'

export function UserSummary() {
  const { user } = useAuth()

  if (!user) return null

  const canManageUsers = user.permissions.includes('USER_READ') || user.permissions.includes('USER_CREATE')
  const canManageKits = user.permissions.includes('KIT_READ') || user.permissions.includes('KIT_CREATE')

  return (
    <section className="user-summary" aria-labelledby="welcome-title">
      <div className="user-summary__hero">
        <div>
          <p className="eyebrow">Painel Principal</p>
          <h1 id="welcome-title">Olá, {user.displayName}</h1>
          <p>Sua sessão está ativa no Glazr. Acesse os módulos disponíveis abaixo.</p>
        </div>
      </div>

      <div className="user-summary__section">
        <h2 className="section-title">Módulos do Sistema</h2>
        <div className="shortcut-grid">
          {canManageUsers ? (
            <Link to="/admin/users" className="shortcut-card">
              <div className="shortcut-card__header">
                <div className="shortcut-card__icon">
                  <Users size={20} strokeWidth={2} />
                </div>
                <span className="shortcut-card__badge">Administração</span>
              </div>
              <h3 className="shortcut-card__title">Funcionários</h3>
              <p className="shortcut-card__desc">
                Gerencie funcionários, envie convites de acesso e configure perfis de permissão.
              </p>
              <div className="shortcut-card__action">
                <span>Acessar módulo</span>
                <ArrowRight size={16} />
              </div>
            </Link>
          ) : null}

          {canManageKits ? (
            <Link to="/admin/kits" className="shortcut-card">
              <div className="shortcut-card__header">
                <div className="shortcut-card__icon">
                  <Package size={20} strokeWidth={2} />
                </div>
                <span className="shortcut-card__badge">Catálogo</span>
              </div>
              <h3 className="shortcut-card__title">Kits e Produtos</h3>
              <p className="shortcut-card__desc">
                Cadastre e consulte kits de vidraçaria, vidros, cores e especificações de componentes.
              </p>
              <div className="shortcut-card__action">
                <span>Acessar módulo</span>
                <ArrowRight size={16} />
              </div>
            </Link>
          ) : null}

          <Link to="/account/security" className="shortcut-card">
            <div className="shortcut-card__header">
              <div className="shortcut-card__icon">
                <Shield size={20} strokeWidth={2} />
              </div>
              <span className="shortcut-card__badge">Segurança</span>
            </div>
            <h3 className="shortcut-card__title">Segurança da Conta</h3>
            <p className="shortcut-card__desc">
              Altere sua senha local e gerencie a autenticação vinculada à sua conta Google.
            </p>
            <div className="shortcut-card__action">
              <span>Configurar segurança</span>
              <ArrowRight size={16} />
            </div>
          </Link>
        </div>
      </div>

      <div className="user-summary__section">
        <h2 className="section-title">Informações da Sessão</h2>
        <div className="session-grid">
          <article className="info-card">
            <p className="info-card__label">Conta Conectada</p>
            <strong>{user.email}</strong>
            <span className="status-badge">Sessão ativa</span>
          </article>
          <article className="info-card">
            <p className="info-card__label">Perfis de Acesso</p>
            <div className="tag-list">
              {user.profiles.map((profile) => (
                <span className="tag" key={profile}>
                  {profile}
                </span>
              ))}
            </div>
          </article>
        </div>
      </div>
    </section>
  )
}
