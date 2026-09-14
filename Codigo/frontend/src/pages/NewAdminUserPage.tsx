import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Check, Mail, Shield, Users } from 'lucide-react'
import { useAuth } from '../features/auth/hooks/use-auth'
import {
  createUser,
  listProfiles,
  type AccessProfile,
  type UserSummary,
} from '../features/users/api/users-api'
import { AppShell } from '../shared/components/AppShell'
import './WorkflowPages.css'

export function NewAdminUserPage() {
  const { user } = useAuth()
  const [profiles, setProfiles] = useState<AccessProfile[]>([])
  const [selected, setSelected] = useState<string[]>([])
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [created, setCreated] = useState<UserSummary | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    void listProfiles()
      .then((page) => {
        const activeProfiles = page.content.filter((profile) => profile.active)
        setProfiles(activeProfiles)
        // Se houver apenas 1 perfil ou padrão, pode pré-selecionar vendedor se existir
        const vendedor = activeProfiles.find((p) => p.name.toUpperCase() === 'VENDEDOR')
        if (vendedor) {
          setSelected([vendedor.id])
        } else if (activeProfiles.length > 0) {
          setSelected([activeProfiles[0].id])
        }
      })
      .catch(() => setError('Não foi possível carregar os perfis de acesso.'))
  }, [])

  function toggleProfile(id: string) {
    setSelected((current) =>
      current.includes(id) ? current.filter((item) => item !== id) : [...current, id],
    )
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (selected.length === 0) {
      setError('Selecione ao menos um perfil de acesso para o funcionário.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const newUser = await createUser({
        email: email.trim(),
        displayName: name.trim(),
        accessProfileIds: selected,
      })
      setCreated(newUser)
    } catch {
      setError('Não foi possível criar o funcionário. Confira os dados e se o e-mail já está em uso.')
    } finally {
      setBusy(false)
    }
  }

  if (!user?.permissions.includes('USER_CREATE') || !user.permissions.includes('ACCESS_PROFILE_ASSIGN')) {
    return (
      <AppShell>
        <section className="workflow-card">
          <h1>Acesso não permitido</h1>
          <p>Você não possui permissão para convidar novos funcionários.</p>
        </section>
      </AppShell>
    )
  }

  return (
    <AppShell>
      <section className="workflow-card workflow-card--narrow">
        <p className="eyebrow">Novo Acesso</p>
        <h1>Convidar funcionário</h1>
        <p>
          O funcionário receberá um convite por e-mail com link temporário para configurar a própria
          senha. Nenhuma senha inicial é cadastrada pelo administrador.
        </p>

        {created ? (
          <div className="workflow-feedback" style={{ marginTop: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', marginBottom: '0.6rem' }}>
              <Mail size={20} color="var(--color-blue-700)" />
              <strong style={{ fontSize: '1.05rem', color: 'var(--color-blue-950)' }}>
                Convite enviado com sucesso para {created.displayName}!
              </strong>
            </div>
            <p style={{ margin: '0 0 1rem', fontSize: '0.9rem', color: 'var(--color-text)' }}>
              E-mail de destino: <strong>{created.email}</strong>
              <br />
              Status de entrega: <strong>{created.invitationDeliveryStatus ?? 'PENDING'}</strong>
              <br />
              Em ambiente de desenvolvimento local, a mensagem pode ser lida pelo Mailpit na porta 8025.
            </p>
            <div className="workflow-actions" style={{ justifyContent: 'flex-start' }}>
              <Link className="button button--primary" to="/admin/users">
                Ver todos os funcionários
              </Link>
              <button
                className="button button--secondary"
                type="button"
                onClick={() => {
                  setCreated(null)
                  setName('')
                  setEmail('')
                }}
              >
                Convidar outro funcionário
              </button>
            </div>
          </div>
        ) : (
          <form className="workflow-form" onSubmit={(event) => void submit(event)}>
            {error ? (
              <p className="workflow-feedback workflow-error" role="alert">
                {error}
              </p>
            ) : null}

            <label>
              Nome completo
              <input
                required
                maxLength={120}
                placeholder="Ex: Carlos Eduardo da Silva"
                value={name}
                onChange={(event) => setName(event.target.value)}
                disabled={busy}
              />
            </label>

            <label>
              E-mail corporativo
              <input
                type="email"
                required
                maxLength={320}
                placeholder="funcionario@glazr.local"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                disabled={busy}
              />
            </label>

            <fieldset style={{ border: 'none', padding: 0, margin: '0.5rem 0 0' }}>
              <legend style={{ fontWeight: 700, marginBottom: '0.5rem', color: 'var(--color-blue-950)' }}>
                Selecione os Perfis de Acesso
              </legend>

              <div className="profile-cards-grid">
                {profiles.map((profile) => {
                  const isSelected = selected.includes(profile.id)
                  const isAdm = profile.name.toUpperCase() === 'ADMINISTRADOR'
                  const title = isAdm ? 'Administrador' : 'Vendedor'
                  const description = isAdm
                    ? 'Acesso irrestrito a configurações, administração de equipe e catálogo de kits.'
                    : 'Acesso a consultas de kits de vidraçaria, produtos e fluxo comercial.'

                  return (
                    <div
                      key={profile.id}
                      role="checkbox"
                      aria-checked={isSelected}
                      tabIndex={0}
                      className={`profile-card ${isSelected ? 'profile-card--selected' : ''}`}
                      onClick={() => toggleProfile(profile.id)}
                      onKeyDown={(e) => {
                        if (e.key === ' ' || e.key === 'Enter') {
                          e.preventDefault()
                          toggleProfile(profile.id)
                        }
                      }}
                    >
                      <div className="profile-card__header">
                        <div className="profile-card__icon">
                          {isAdm ? <Shield size={18} /> : <Users size={18} />}
                        </div>
                        {isSelected ? (
                          <span
                            style={{
                              display: 'inline-flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              width: '1.25rem',
                              height: '1.25rem',
                              borderRadius: 'var(--radius-xs)',
                              background: 'var(--color-blue-700)',
                              color: '#fff',
                            }}
                          >
                            <Check size={12} strokeWidth={3} />
                          </span>
                        ) : null}
                      </div>
                      <h4 className="profile-card__title">{title}</h4>
                      <p className="profile-card__desc">{description}</p>
                    </div>
                  )
                })}
              </div>
            </fieldset>

            <div className="workflow-actions" style={{ marginTop: '0.75rem' }}>
              <button
                className="button button--primary"
                type="submit"
                disabled={busy || selected.length === 0}
              >
                {busy ? 'Enviando convite...' : 'Criar e enviar convite'}
              </button>
              <Link className="button button--secondary" to="/admin/users">
                Cancelar
              </Link>
            </div>
          </form>
        )}
      </section>
    </AppShell>
  )
}

