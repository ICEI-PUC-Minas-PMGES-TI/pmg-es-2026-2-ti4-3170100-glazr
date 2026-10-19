import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { MailX, Pencil, Search, Send, UserCheck, UserPlus, UserX } from 'lucide-react'
import { useAuth } from '../features/auth/hooks/use-auth'
import {
  listUsers,
  resendInvitation,
  revokeInvitation,
  updateUserStatus,
  type UserSummary,
} from '../features/users/api/users-api'
import { ActionsMenu, type ActionsMenuItem } from '../shared/components/ActionsMenu'
import { AppShell } from '../shared/components/AppShell'
import './WorkflowPages.css'

export function AdminUsersPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [users, setUsers] = useState<UserSummary[]>([])
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL')
  const [profileFilter, setProfileFilter] = useState<'ALL' | 'ADMINISTRADOR' | 'VENDEDOR'>('ALL')

  const load = useCallback(() => {
    return listUsers()
      .then((page) => setUsers(page.content))
      .catch(() => setError('Não foi possível carregar os funcionários.'))
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  async function act(id: string, operation: () => Promise<unknown>, successMessage?: string) {
    setBusy(id)
    setError(null)
    setSuccess(null)
    try {
      await operation()
      if (successMessage) setSuccess(successMessage)
      await load()
    } catch (err: unknown) {
      const isLastAdmin =
        typeof err === 'object' &&
        err !== null &&
        'code' in err &&
        (err as { code: string }).code === 'LAST_ACTIVE_ADMINISTRATOR'
      if (isLastAdmin) {
        setError('Não é possível inativar o último administrador ativo do sistema.')
      } else {
        setError('Não foi possível concluir a ação.')
      }
    } finally {
      setBusy(null)
    }
  }

  // Filtragem no cliente para responsividade instantânea
  const filteredUsers = useMemo(() => {
    return users.filter((item) => {
      // Filtro de texto (nome ou e-mail)
      if (search.trim()) {
        const query = search.trim().toLowerCase()
        const matchesName = item.displayName.toLowerCase().includes(query)
        const matchesEmail = item.email.toLowerCase().includes(query)
        if (!matchesName && !matchesEmail) return false
      }

      // Filtro por status
      if (statusFilter === 'ACTIVE' && !item.active) return false
      if (statusFilter === 'INACTIVE' && item.active) return false

      // Filtro por perfil
      if (profileFilter !== 'ALL') {
        const hasProfile = item.accessProfiles.some((p) => p.name.toUpperCase() === profileFilter)
        if (!hasProfile) return false
      }

      return true
    })
  }, [users, search, statusFilter, profileFilter])

  function actionsFor(item: UserSummary): ActionsMenuItem[] {
    const permissions = user?.permissions ?? []
    const actions: ActionsMenuItem[] = []
    if (permissions.includes('USER_UPDATE')) {
      actions.push({
        label: 'Editar',
        icon: <Pencil size={16} aria-hidden="true" />,
        onSelect: () => void navigate(`/admin/users/${item.id}/edit`),
      })
      if (!item.authenticationMethods.length) {
        actions.push(
          {
            label: 'Reenviar convite',
            icon: <Send size={16} aria-hidden="true" />,
            onSelect: () =>
              void act(item.id, () => resendInvitation(item.id), `Convite reenviado para ${item.email}.`),
          },
          {
            label: 'Revogar convite',
            icon: <MailX size={16} aria-hidden="true" />,
            tone: 'danger',
            onSelect: () =>
              void act(item.id, () => revokeInvitation(item.id), `Convite de ${item.displayName} revogado.`),
          },
        )
      }
    }
    if (permissions.includes('USER_STATUS_UPDATE')) {
      actions.push({
        label: item.active ? 'Inativar' : 'Ativar',
        icon: item.active ? <UserX size={16} aria-hidden="true" /> : <UserCheck size={16} aria-hidden="true" />,
        tone: item.active ? 'danger' : 'default',
        onSelect: () =>
          void act(
            item.id,
            () => updateUserStatus(item.id, !item.active),
            `Status de ${item.displayName} atualizado para ${!item.active ? 'Ativo' : 'Inativo'}.`,
          ),
      })
    }
    return actions
  }

  // Contadores
  const totalCount = users.length
  const activeCount = users.filter((u) => u.active).length
  const inactiveCount = totalCount - activeCount

  if (!user?.permissions.includes('USER_READ')) {
    return (
      <AppShell>
        <section className="workflow-card">
          <h1>Acesso não permitido</h1>
          <p>Você não possui permissão para consultar funcionários.</p>
        </section>
      </AppShell>
    )
  }

  return (
    <AppShell>
      <section className="workflow-card">
        <div className="workflow-actions">
          <div>
            <p className="eyebrow">Administração de Acessos</p>
            <h1>Funcionários</h1>
          </div>
          {user.permissions.includes('USER_CREATE') ? (
            <Link className="button button--primary" to="/admin/users/new">
              <UserPlus size={18} style={{ marginRight: '0.45rem' }} />
              Convidar funcionário
            </Link>
          ) : null}
        </div>

        {/* ── Barra de Busca e Filtros ── */}
        <div className="filter-bar">
          <div className="filter-search">
            <Search size={16} className="filter-search__icon" />
            <input
              type="search"
              placeholder="Buscar por nome ou e-mail..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="Buscar funcionários"
            />
          </div>

          <div className="filter-controls">
            <select
              className="filter-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as 'ALL' | 'ACTIVE' | 'INACTIVE')}
              aria-label="Filtrar por status"
            >
              <option value="ALL">Todos os status</option>
              <option value="ACTIVE">Apenas ativos</option>
              <option value="INACTIVE">Apenas inativos</option>
            </select>

            <select
              className="filter-select"
              value={profileFilter}
              onChange={(e) => setProfileFilter(e.target.value as 'ALL' | 'ADMINISTRADOR' | 'VENDEDOR')}
              aria-label="Filtrar por perfil"
            >
              <option value="ALL">Todos os perfis</option>
              <option value="ADMINISTRADOR">Administrador</option>
              <option value="VENDEDOR">Vendedor</option>
            </select>

            <div className="item-counter">
              <span>
                Total: <strong>{totalCount}</strong> ({activeCount} ativos, {inactiveCount} inativos)
                {filteredUsers.length !== totalCount ? ` · Exibindo ${filteredUsers.length}` : ''}
              </span>
            </div>
          </div>
        </div>

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

        {/* ── Tabela Translúcida de Funcionários ── */}
        <table className="data-table">
          <thead>
            <tr>
              <th>Funcionário</th>
              <th>Status</th>
              <th>Perfis de Acesso</th>
              <th>Métodos de Acesso</th>
              <th>Convite</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: '2rem' }}>
                  Nenhum funcionário encontrado para os filtros selecionados.
                </td>
              </tr>
            ) : (
              filteredUsers.map((item) => (
                <tr key={item.id}>
                  <td>
                    <strong>{item.displayName}</strong>
                    <br />
                    <span style={{ color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>{item.email}</span>
                  </td>
                  <td>
                    <span className={`badge ${item.active ? 'badge--active' : 'badge--inactive'}`}>
                      {item.active ? 'Ativo' : 'Inativo'}
                    </span>
                  </td>
                  <td>
                    {item.accessProfiles.map((profile) => {
                      const isAdm = profile.name.toUpperCase() === 'ADMINISTRADOR'
                      const isVend = profile.name.toUpperCase() === 'VENDEDOR'
                      const badgeClass = isAdm ? 'badge--admin' : isVend ? 'badge--seller' : ''
                      return (
                        <span className={`badge ${badgeClass}`} key={profile.id}>
                          {profile.name}
                        </span>
                      )
                    })}
                  </td>
                  <td>
                    {item.authenticationMethods.length ? (
                      item.authenticationMethods.map((method) => (
                        <span className="badge" key={method}>
                          {method === 'PASSWORD' ? 'Senha' : method === 'GOOGLE' ? 'Google' : method}
                        </span>
                      ))
                    ) : (
                      <span style={{ color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>
                        Pendente de ativação
                      </span>
                    )}
                  </td>
                  <td>
                    {item.invitationStatus ? (
                      <>
                        <span className="badge">{item.invitationStatus}</span>
                        {item.invitationDeliveryStatus ? (
                          <span style={{ fontSize: '0.78rem', color: 'var(--color-text-muted)' }}>
                            <br />
                            Envio: {item.invitationDeliveryStatus}
                          </span>
                        ) : null}
                        {item.invitationExpiresAt ? (
                          <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'block' }}>
                            Expira: {new Date(item.invitationExpiresAt).toLocaleString('pt-BR')}
                          </span>
                        ) : null}
                      </>
                    ) : (
                      <span style={{ color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>—</span>
                    )}
                  </td>
                  <td>
                    <ActionsMenu
                      label={`Ações para ${item.displayName}`}
                      disabled={busy === item.id}
                      items={actionsFor(item)}
                    />
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </section>
    </AppShell>
  )
}

