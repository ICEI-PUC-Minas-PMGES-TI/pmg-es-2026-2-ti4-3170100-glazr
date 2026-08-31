import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../features/auth/hooks/use-auth'
import { listUsers, resendInvitation, revokeInvitation, updateUserStatus, type UserSummary } from '../features/users/api/users-api'
import { AppShell } from '../shared/components/AppShell'
import './WorkflowPages.css'

export function AdminUsersPage() {
  const { user } = useAuth(); const [users, setUsers] = useState<UserSummary[]>([]); const [error, setError] = useState<string | null>(null); const [busy, setBusy] = useState<string | null>(null)
  const load = useCallback(() => listUsers().then((page) => setUsers(page.content)).catch(() => setError('Não foi possível carregar os funcionários.')), [])
  useEffect(() => { void load() }, [load])
  async function act(id: string, operation: () => Promise<unknown>) { setBusy(id); setError(null); try { await operation(); await load() } catch { setError('Não foi possível concluir a ação.') } finally { setBusy(null) } }
  if (!user?.permissions.includes('USER_READ')) return <AppShell><section className="workflow-card"><h1>Acesso não permitido</h1></section></AppShell>
  return <AppShell><section className="workflow-card"><div className="workflow-actions"><div><p className="eyebrow">Administração</p><h1>Funcionários</h1></div>{user.permissions.includes('USER_CREATE') ? <Link className="button button--primary" to="/admin/users/new">Convidar funcionário</Link> : null}</div>
    {error ? <p className="workflow-feedback workflow-error" role="alert">{error}</p> : null}
    <table className="data-table"><thead><tr><th>Funcionário</th><th>Perfis</th><th>Acesso</th><th>Convite</th><th>Ações</th></tr></thead><tbody>{users.map((item) => <tr key={item.id}><td><strong>{item.displayName}</strong><br /><span>{item.email}</span><br /><span className="badge">{item.active ? 'Ativo' : 'Inativo'}</span></td><td>{item.accessProfiles.map((profile) => <span className="badge" key={profile.id}>{profile.name}</span>)}</td><td>{item.authenticationMethods.length ? item.authenticationMethods.map((method) => <span className="badge" key={method}>{method}</span>) : 'Ainda não configurado'}</td><td>{item.invitationStatus ?? '—'}{item.invitationDeliveryStatus ? ` · ${item.invitationDeliveryStatus}` : ''}<br />{item.invitationExpiresAt ? new Date(item.invitationExpiresAt).toLocaleString('pt-BR') : null}</td><td><div className="workflow-actions">{user.permissions.includes('USER_UPDATE') && !item.authenticationMethods.length ? <><button className="button button--secondary" disabled={busy === item.id} onClick={() => void act(item.id, () => resendInvitation(item.id))}>Reenviar</button><button className="button button--secondary" disabled={busy === item.id} onClick={() => void act(item.id, () => revokeInvitation(item.id))}>Revogar</button></> : null}{user.permissions.includes('USER_STATUS_UPDATE') ? <button className="button button--secondary" disabled={busy === item.id} onClick={() => void act(item.id, () => updateUserStatus(item.id, !item.active))}>{item.active ? 'Desativar' : 'Ativar'}</button> : null}</div></td></tr>)}</tbody></table>
  </section></AppShell>
}
