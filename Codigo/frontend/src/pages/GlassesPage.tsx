import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { CircleCheck, CircleOff, Edit3, Plus, Search, Trash2 } from 'lucide-react'
import { useAuth } from '../features/auth/hooks/use-auth'
import {
  deleteGlass,
  listGlasses,
  updateGlassStatus,
  type GlassSummary,
} from '../features/glasses/api/glasses-api'
import { ActionsMenu, type ActionsMenuItem } from '../shared/components/ActionsMenu'
import { AppShell } from '../shared/components/AppShell'
import { currencyFormatter } from '../shared/format/formatters'
import './WorkflowPages.css'

const FINISH_OPTIONS = [
  'Lapidado',
  'Bisotê',
  'Canto Moeda',
  'Jateado',
  'Sem acabamento',
]

const THICKNESS_OPTIONS = [3, 4, 5, 6, 8, 10, 12, 15, 19]

export function GlassesPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [glasses, setGlasses] = useState<GlassSummary[]>([])
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [busy, setBusy] = useState<string | null>(null)

  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL')
  const [thicknessFilter, setThicknessFilter] = useState<string>('ALL')
  const [finishFilter, setFinishFilter] = useState<string>('ALL')

  const load = useCallback(() => {
    return listGlasses()
      .then((page) => setGlasses(page.content))
      .catch(() => setError('Não foi possível carregar os vidros do catálogo.'))
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
    } catch {
      setError('Não foi possível concluir a ação no vidro.')
    } finally {
      setBusy(null)
    }
  }

  const filteredGlasses = useMemo(() => {
    return glasses.filter((item) => {
      if (search.trim()) {
        const query = search.trim().toLowerCase()
        const matchesName = item.name.toLowerCase().includes(query)
        const matchesFinish = item.finish.toLowerCase().includes(query)
        const matchesColor = item.color.toLowerCase().includes(query)
        if (!matchesName && !matchesFinish && !matchesColor) return false
      }

      if (statusFilter === 'ACTIVE' && !item.active) return false
      if (statusFilter === 'INACTIVE' && item.active) return false

      if (thicknessFilter !== 'ALL') {
        const targetThickness = Number(thicknessFilter)
        if (item.thicknessMm !== targetThickness) return false
      }

      if (finishFilter !== 'ALL') {
        if (item.finish.toLowerCase() !== finishFilter.toLowerCase()) return false
      }

      return true
    })
  }, [glasses, search, statusFilter, thicknessFilter, finishFilter])

  function actionsFor(item: GlassSummary): ActionsMenuItem[] {
    const permissions = user?.permissions ?? []
    const actions: ActionsMenuItem[] = []
    if (permissions.includes('GLASS_UPDATE')) {
      actions.push(
        {
          label: 'Editar',
          icon: <Edit3 size={16} aria-hidden="true" />,
          onSelect: () => void navigate(`/glasses/${item.id}/edit`),
        },
        {
          label: item.active ? 'Inativar' : 'Ativar',
          icon: item.active ? <CircleOff size={16} aria-hidden="true" /> : <CircleCheck size={16} aria-hidden="true" />,
          onSelect: () =>
            void act(
              item.id,
              () => updateGlassStatus(item.id, !item.active),
              `Status do vidro "${item.name}" alterado para ${!item.active ? 'Ativo' : 'Inativo'}.`,
            ),
        },
      )
    }
    if (permissions.includes('GLASS_DELETE')) {
      actions.push({
        label: 'Excluir',
        icon: <Trash2 size={16} aria-hidden="true" />,
        tone: 'danger',
        onSelect: () => {
          if (window.confirm(`Deseja realmente remover o vidro "${item.name}" (${item.thicknessMm}mm - ${item.finish})?`)) {
            void act(item.id, () => deleteGlass(item.id), `Vidro "${item.name}" excluído com sucesso.`)
          }
        },
      })
    }
    return actions
  }

  const totalCount = glasses.length
  const activeCount = glasses.filter((g) => g.active).length
  const inactiveCount = totalCount - activeCount

  if (!user?.permissions.includes('GLASS_READ')) {
    return (
      <AppShell>
        <section className="workflow-card">
          <h1>Acesso não permitido</h1>
          <p>Você não possui permissão para consultar o catálogo de vidros.</p>
        </section>
      </AppShell>
    )
  }

  return (
    <AppShell>
      <section className="workflow-card">
        <div className="workflow-actions">
          <div>
            <p className="eyebrow">Catálogo de Produtos</p>
            <h1>Vidros</h1>
          </div>
          {user.permissions.includes('GLASS_CREATE') ? (
            <Link className="button button--primary" to="/glasses/new">
              <Plus size={18} style={{ marginRight: '0.45rem' }} />
              Novo vidro
            </Link>
          ) : null}
        </div>

        <div className="filter-bar">
          <div className="filter-search">
            <Search size={16} className="filter-search__icon" />
            <input
              type="search"
              placeholder="Buscar por nome, acabamento ou cor..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="Buscar vidros"
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
              value={thicknessFilter}
              onChange={(e) => setThicknessFilter(e.target.value)}
              aria-label="Filtrar por espessura"
            >
              <option value="ALL">Todas as espessuras</option>
              {THICKNESS_OPTIONS.map((t) => (
                <option key={t} value={String(t)}>
                  {t} mm
                </option>
              ))}
            </select>

            <select
              className="filter-select"
              value={finishFilter}
              onChange={(e) => setFinishFilter(e.target.value)}
              aria-label="Filtrar por acabamento"
            >
              <option value="ALL">Todos os acabamentos</option>
              {FINISH_OPTIONS.map((f) => (
                <option key={f} value={f}>
                  {f}
                </option>
              ))}
            </select>

            <div className="item-counter">
              <span>
                Total: <strong>{totalCount}</strong> ({activeCount} ativos, {inactiveCount} inativos)
                {filteredGlasses.length !== totalCount ? ` · Exibindo ${filteredGlasses.length}` : ''}
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

        <table className="data-table">
          <thead>
            <tr>
              <th>Vidro / Especificação</th>
              <th>Espessura</th>
              <th>Acabamento</th>
              <th>Cor</th>
              <th>Preço (m²)</th>
              <th>Status</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {filteredGlasses.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center', padding: '2rem' }}>
                  Nenhum vidro encontrado para os filtros selecionados.
                </td>
              </tr>
            ) : (
              filteredGlasses.map((item) => (
                <tr key={item.id}>
                  <td>
                    <strong>{item.name}</strong>
                  </td>
                  <td>
                    <span className="badge badge--admin">{item.thicknessMm} mm</span>
                  </td>
                  <td>
                    <span className="badge">{item.finish}</span>
                  </td>
                  <td>
                    <span className="badge">{item.color}</span>
                  </td>
                  <td>
                    <strong>{currencyFormatter.format(item.pricePerSquareMeter)}/m²</strong>
                  </td>
                  <td>
                    <span className={`badge ${item.active ? 'badge--active' : 'badge--inactive'}`}>
                      {item.active ? 'Ativo' : 'Inativo'}
                    </span>
                  </td>
                  <td>
                    <ActionsMenu
                      label={`Ações para ${item.name}`}
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
