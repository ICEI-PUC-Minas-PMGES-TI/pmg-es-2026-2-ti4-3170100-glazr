import { useAuth } from '../hooks/use-auth'

export function UserSummary() {
  const { isSubmitting, logout, user } = useAuth()

  if (!user) return null

  return (
    <section className="user-summary" aria-labelledby="welcome-title">
      <div className="user-summary__hero">
        <div>
          <p className="eyebrow">Área de trabalho</p>
          <h1 id="welcome-title">Olá, {user.displayName}</h1>
          <p>Sua sessão está ativa. O Glazr está pronto para a próxima etapa.</p>
        </div>
        <button className="button button--secondary" type="button" onClick={() => void logout()} disabled={isSubmitting}>
          {isSubmitting ? 'Saindo...' : 'Sair'}
        </button>
      </div>

      <div className="user-summary__grid">
        <article className="info-card">
          <p className="info-card__label">Conta conectada</p>
          <strong>{user.email}</strong>
          <span className="status-badge">Ativa</span>
        </article>
        <article className="info-card">
          <p className="info-card__label">Perfis de acesso</p>
          <div className="tag-list">
            {user.profiles.map((profile) => (
              <span className="tag" key={profile}>
                {profile}
              </span>
            ))}
          </div>
        </article>
        <article className="info-card info-card--wide">
          <p className="info-card__label">Permissões disponíveis</p>
          <div className="permission-list">
            {user.permissions.length > 0 ? (
              user.permissions.map((permission) => <span key={permission}>{permission}</span>)
            ) : (
              <span>Nenhuma permissão adicional.</span>
            )}
          </div>
        </article>
      </div>
    </section>
  )
}
