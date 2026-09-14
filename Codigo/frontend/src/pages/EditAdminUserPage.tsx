import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Check, Shield, Users } from 'lucide-react'
import { useAuth } from '../features/auth/hooks/use-auth'
import {
  getUser,
  listProfiles,
  updateUser,
  updateUserStatus,
  type AccessProfile,
  type UserSummary,
} from '../features/users/api/users-api'
import { AppShell } from '../shared/components/AppShell'
import './WorkflowPages.css'

export function EditAdminUserPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [profiles, setProfiles] = useState<AccessProfile[]>([])
  const [targetUser, setTargetUser] = useState<UserSummary | null>(null)
  const [selected, setSelected] = useState<string[]>([])
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [active, setActive] = useState(true)
  const [originalActive, setOriginalActive] = useState(true)

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!id) return
    Promise.all([getUser(id), listProfiles()])
      .then(([userData, profilesData]) => {
        setTargetUser(userData)
        setName(userData.displayName)
        setEmail(userData.email)
        setActive(userData.active)
        setOriginalActive(userData.active)
        setSelected(userData.accessProfiles.map((p) => p.id))
        setProfiles(profilesData.content.filter((p) => p.active))
      })
      .catch(() => setError('Não foi possível carregar os dados do funcionário.'))
      .finally(() => setLoading(false))
  }, [id])

  function toggleProfile(profileId: string) {
    setSelected((current) =>
      current.includes(profileId) ? current.filter((item) => item !== profileId) : [...current, profileId],
    )
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!id) return
    if (selected.length === 0) {
      setError('Selecione ao menos um perfil de acesso para o funcionário.')
      return
    }

    setBusy(true)
    setError(null)
    setSuccess(null)

    try {
      // 1. Atualizar dados cadastrais e perfis
      await updateUser(id, {
        email: email.trim(),
        displayName: name.trim(),
        accessProfileIds: selected,
      })

      // 2. Atualizar status se tiver sido alterado
      if (active !== originalActive) {
        await updateUserStatus(id, active)
        setOriginalActive(active)
      }

      setSuccess('Dados do funcionário atualizados com sucesso!')
      setTimeout(() => {
        void navigate('/admin/users')
      }, 1200)
    } catch (err: unknown) {
      const isLastAdmin =
        typeof err === 'object' &&
        err !== null &&
        'code' in err &&
        (err as { code: string }).code === 'LAST_ACTIVE_ADMINISTRATOR'

      if (isLastAdmin) {
        setError('Não é possível inativar ou remover o perfil do último administrador ativo do sistema.')
      } else {
        setError('Não foi possível atualizar o funcionário. Verifique os dados informados.')
      }
    } finally {
      setBusy(false)
    }
  }

  if (!user?.permissions.includes('USER_UPDATE')) {
    return (
      <AppShell>
        <section className="workflow-card">
          <h1>Acesso não permitido</h1>
          <p>Você não possui permissão para editar funcionários.</p>
        </section>
      </AppShell>
    )
  }

  if (loading) {
    return (
      <AppShell>
        <section className="workflow-card workflow-card--narrow">
          <p>Carregando dados do funcionário...</p>
        </section>
      </AppShell>
    )
  }

  if (!targetUser) {
    return (
      <AppShell>
        <section className="workflow-card workflow-card--narrow">
          <h1>Funcionário não encontrado</h1>
          <p>O funcionário solicitado não existe ou foi removido.</p>
          <Link className="button button--secondary" to="/admin/users">
            Voltar para funcionários
          </Link>
        </section>
      </AppShell>
    )
  }

  return (
    <AppShell>
      <section className="workflow-card workflow-card--narrow">
        <p className="eyebrow">Edição de Acesso</p>
        <h1>Editar funcionário</h1>
        <p>Atualize os dados, perfis de permissão e status de acesso da conta.</p>

        {error ? (
          <p className="workflow-feedback workflow-error" role="alert">
            {error}
          </p>
        ) : null}

        {success ? (
          <p className="workflow-feedback" role="status">
            {success}
          </p>
        ) : null}

        <form className="workflow-form" onSubmit={(event) => void submit(event)}>
          <label>
            Nome completo
            <input
              required
              maxLength={120}
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
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              disabled={busy}
            />
          </label>

          <label>
            Status do acesso
            <select
              className="filter-select"
              value={active ? 'true' : 'false'}
              onChange={(event) => setActive(event.target.value === 'true')}
              disabled={busy}
            >
              <option value="true">Ativo (Permitir acesso ao sistema)</option>
              <option value="false">Inativo (Bloquear acesso e encerrar sessões)</option>
            </select>
          </label>

          <fieldset style={{ border: 'none', padding: 0, margin: '0.5rem 0 0' }}>
            <legend style={{ fontWeight: 700, marginBottom: '0.5rem', color: 'var(--color-blue-950)' }}>
              Perfis de Acesso
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
              {busy ? 'Salvando alterações...' : 'Salvar alterações'}
            </button>
            <Link className="button button--secondary" to="/admin/users">
              Cancelar
            </Link>
          </div>
        </form>
      </section>
    </AppShell>
  )
}
