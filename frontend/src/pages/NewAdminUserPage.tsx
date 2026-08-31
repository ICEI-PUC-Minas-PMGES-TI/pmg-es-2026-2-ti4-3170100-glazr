import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../features/auth/hooks/use-auth'
import { createUser, listProfiles, type AccessProfile, type UserSummary } from '../features/users/api/users-api'
import { AppShell } from '../shared/components/AppShell'
import './WorkflowPages.css'

export function NewAdminUserPage() {
  const { user } = useAuth(); const [profiles, setProfiles] = useState<AccessProfile[]>([]); const [selected, setSelected] = useState<string[]>([]); const [email, setEmail] = useState(''); const [name, setName] = useState(''); const [created, setCreated] = useState<UserSummary | null>(null); const [error, setError] = useState<string | null>(null); const [busy, setBusy] = useState(false)
  useEffect(() => { void listProfiles().then((page) => setProfiles(page.content.filter((profile) => profile.active))).catch(() => setError('Não foi possível carregar os perfis.')) }, [])
  async function submit(event: FormEvent) { event.preventDefault(); setBusy(true); setError(null); try { setCreated(await createUser({ email: email.trim(), displayName: name.trim(), accessProfileIds: selected })) } catch { setError('Não foi possível criar o funcionário. Confira os dados e se o e-mail já está em uso.') } finally { setBusy(false) } }
  if (!user?.permissions.includes('USER_CREATE') || !user.permissions.includes('ACCESS_PROFILE_ASSIGN')) return <AppShell><section className="workflow-card"><h1>Acesso não permitido</h1></section></AppShell>
  return <AppShell><section className="workflow-card workflow-card--narrow"><p className="eyebrow">Novo acesso</p><h1>Convidar funcionário</h1><p>O funcionário receberá um convite temporário para configurar o próprio acesso. Nenhuma senha é definida aqui.</p>
    {created ? <div className="workflow-feedback"><strong>{created.displayName} foi cadastrado.</strong><br />Envio: {created.invitationDeliveryStatus ?? 'PENDING'}. <Link to="/admin/users">Ver funcionários</Link></div> : <form className="workflow-form" onSubmit={(event) => void submit(event)}>{error ? <p className="workflow-feedback workflow-error" role="alert">{error}</p> : null}<label>Nome<input required maxLength={120} value={name} onChange={(event) => setName(event.target.value)} /></label><label>E-mail<input type="email" required maxLength={320} value={email} onChange={(event) => setEmail(event.target.value)} /></label><fieldset><legend>Perfis de acesso</legend><div className="profile-options">{profiles.map((profile) => <label key={profile.id}><input type="checkbox" checked={selected.includes(profile.id)} onChange={(event) => setSelected((current) => event.target.checked ? [...current, profile.id] : current.filter((id) => id !== profile.id))} />{profile.name}</label>)}</div></fieldset><button className="button button--primary" disabled={busy || selected.length === 0}>{busy ? 'Criando...' : 'Criar e enviar convite'}</button></form>}
  </section></AppShell>
}
