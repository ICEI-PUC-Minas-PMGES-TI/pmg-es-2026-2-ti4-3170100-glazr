import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { AlertTriangle, ArrowRight, Search } from 'lucide-react'
import { useAuth } from '../features/auth/hooks/use-auth'
import { listInventory, type GlassStockSummary, type StockStatus } from '../features/inventory/api/inventory-api'
import { AppShell } from '../shared/components/AppShell'
import './WorkflowPages.css'
import './InventoryPages.css'

const areaFormatter = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const statusLabels: Readonly<Record<StockStatus, string>> = {
  OK: 'Regular',
  LOW: 'Estoque baixo',
  UNCONFIGURED: 'Mínimo não configurado',
}

function statusClass(status: StockStatus): string {
  if (status === 'LOW') return 'badge--low'
  if (status === 'UNCONFIGURED') return 'badge--unconfigured'
  return 'badge--active'
}

export function InventoryPage() {
  const { user } = useAuth()
  const [items, setItems] = useState<ReadonlyArray<GlassStockSummary>>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | StockStatus>('ALL')

  useEffect(() => {
    let current = true
    void listInventory()
      .then((page) => {
        if (current) setItems(page.content)
      })
      .catch(() => {
        if (current) setError('Não foi possível carregar o estoque de chapas.')
      })
      .finally(() => {
        if (current) setLoading(false)
      })
    return () => {
      current = false
    }
  }, [])

  const visibleItems = useMemo(() => items.filter((item) => {
    const normalizedQuery = query.trim().toLocaleLowerCase('pt-BR')
    const matchesQuery = !normalizedQuery || [item.glassName, item.finish, item.color]
      .some((value) => value.toLocaleLowerCase('pt-BR').includes(normalizedQuery))
    return matchesQuery && (statusFilter === 'ALL' || item.status === statusFilter)
  }), [items, query, statusFilter])

  const totalArea = items.reduce((sum, item) => sum + item.availableAreaM2, 0)
  const lowCount = items.filter((item) => item.status === 'LOW').length
  const unconfiguredCount = items.filter((item) => item.status === 'UNCONFIGURED').length

  if (!user?.permissions.includes('GLASS_READ')) {
    return <AppShell><section className="workflow-card"><h1>Acesso não permitido</h1><p>Você não possui permissão para consultar o estoque.</p></section></AppShell>
  }

  return (
    <AppShell>
      <section className="workflow-card">
        <div className="workflow-actions">
          <div>
            <p className="eyebrow">Materiais</p>
            <h1>Estoque de chapas</h1>
            <p>Consulte o saldo físico por vidro, acompanhe mínimos e registre o aproveitamento de cada corte.</p>
          </div>
        </div>

        <div className="inventory-metrics" aria-label="Resumo do estoque">
          <div className="inventory-metric"><span className="inventory-metric__label">Área disponível</span><strong className="inventory-metric__value">{areaFormatter.format(totalArea)} m²</strong></div>
          <div className="inventory-metric"><span className="inventory-metric__label">Estoque baixo</span><strong className="inventory-metric__value">{lowCount}</strong></div>
          <div className="inventory-metric"><span className="inventory-metric__label">Sem mínimo</span><strong className="inventory-metric__value">{unconfiguredCount}</strong></div>
        </div>

        {lowCount > 0 ? <p className="workflow-feedback workflow-error" role="alert"><AlertTriangle size={16} aria-hidden="true" /> {lowCount} tipo(s) de vidro atingiram o estoque mínimo.</p> : null}
        {error ? <p className="workflow-feedback workflow-error" role="alert">{error}</p> : null}

        <div className="filter-bar">
          <div className="filter-search"><Search size={16} className="filter-search__icon" /><input aria-label="Buscar estoque" type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Buscar por vidro, acabamento ou cor..." /></div>
          <div className="filter-controls"><select className="filter-select" aria-label="Filtrar situação do estoque" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as 'ALL' | StockStatus)}><option value="ALL">Todas as situações</option><option value="LOW">Estoque baixo</option><option value="OK">Regular</option><option value="UNCONFIGURED">Mínimo não configurado</option></select></div>
        </div>

        <table className="data-table">
          <thead><tr><th>Vidro</th><th>Saldo disponível</th><th>Estoque mínimo</th><th>Situação</th><th>Ação</th></tr></thead>
          <tbody>
            {loading ? <tr><td colSpan={5}>Carregando estoque...</td></tr> : null}
            {!loading && visibleItems.length === 0 ? <tr><td colSpan={5}>Nenhum vidro encontrado para os filtros selecionados.</td></tr> : null}
            {!loading ? visibleItems.map((item) => <tr key={item.glassId}>
              <td><strong>{item.glassName}</strong><br /><span className="badge">{item.thicknessMm} mm · {item.finish} · {item.color}</span></td>
              <td>{areaFormatter.format(item.availableAreaM2)} m²</td>
              <td>{item.minimumAreaM2 === null ? 'Não configurado' : `${areaFormatter.format(item.minimumAreaM2)} m²`}</td>
              <td><span className={`badge ${statusClass(item.status)}`}>{statusLabels[item.status]}</span></td>
              <td><Link className="button button--secondary inventory-sheet-action" to={`/inventory/glasses/${item.glassId}`}>Detalhar <ArrowRight size={15} /></Link></td>
            </tr>) : null}
          </tbody>
        </table>
      </section>
    </AppShell>
  )
}
