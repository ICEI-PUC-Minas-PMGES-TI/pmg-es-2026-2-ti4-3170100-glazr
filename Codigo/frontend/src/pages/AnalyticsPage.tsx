import { useEffect, useMemo, useState, type ReactNode } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { useAuth } from '../features/auth/hooks/use-auth'
import { listGlasses, type GlassSummary } from '../features/glasses/api/glasses-api'
import { listInventory, type GlassStockSummary, type StockStatus } from '../features/inventory/api/inventory-api'
import { listKits, type KitSummary } from '../features/kits/api/kits-api'
import { AppShell } from '../shared/components/AppShell'
import { areaFormatter, currencyFormatter } from '../shared/format/formatters'
import './WorkflowPages.css'
import './InventoryPages.css'
import './AnalyticsPage.css'

const MAX_GLASSES_IN_AREA_CHART = 10

const AXIS_COLOR = '#5f7283'
const GRID_COLOR = '#ebe7fb'
const CURSOR_FILL = 'rgb(124 92 255 / 8%)'

// Cada gráfico ganha seu gradiente, combinando com a faixa colorida do card
const chartGradients = {
  area: ['#0ea5c6', '#7c5cff'],
  finish: ['#7c5cff', '#e0458f'],
  price: ['#f2a20c', '#f2643d'],
} as const

function BarGradient({ id, colors, vertical = false }: Readonly<{ id: string; colors: readonly [string, string]; vertical?: boolean }>) {
  return (
    <defs>
      <linearGradient id={id} x1="0" y1={vertical ? '1' : '0'} x2={vertical ? '0' : '1'} y2="0">
        <stop offset="0%" stopColor={colors[0]} />
        <stop offset="100%" stopColor={colors[1]} />
      </linearGradient>
    </defs>
  )
}

const statusLabels: Readonly<Record<StockStatus, string>> = {
  OK: 'Regular',
  LOW: 'Estoque baixo',
  UNCONFIGURED: 'Mínimo não configurado',
}

const statusColors: Readonly<Record<StockStatus, string>> = {
  OK: '#14a37f',
  LOW: '#f2643d',
  UNCONFIGURED: '#f2a20c',
}

const STATUS_ORDER: ReadonlyArray<StockStatus> = ['OK', 'LOW', 'UNCONFIGURED']

interface ChartRow {
  readonly label: string
  readonly value: number
}

interface ChartCardProps {
  readonly title: string
  readonly description: string
  readonly rows: ReadonlyArray<ChartRow>
  readonly valueLabel: string
  readonly formatValue: (value: number) => string
  readonly emptyMessage: string
  readonly children: ReactNode
}

function ChartCard({ title, description, rows, valueLabel, formatValue, emptyMessage, children }: ChartCardProps) {
  return (
    <figure className="analytics-chart">
      <figcaption>
        <h2>{title}</h2>
        <p>{description}</p>
      </figcaption>
      {rows.length === 0 ? (
        <p className="inventory-empty">{emptyMessage}</p>
      ) : (
        <>
          <div className="analytics-chart__plot">{children}</div>
          <details className="analytics-chart__table">
            <summary>Ver dados em tabela</summary>
            <table className="data-table">
              <thead>
                <tr><th>Item</th><th>{valueLabel}</th></tr>
              </thead>
              <tbody>
                {rows.map((row) => <tr key={row.label}><td>{row.label}</td><td>{formatValue(row.value)}</td></tr>)}
              </tbody>
            </table>
          </details>
        </>
      )}
    </figure>
  )
}

function formatCount(value: number): string {
  return String(value)
}

function formatArea(value: number): string {
  return `${areaFormatter.format(value)} m²`
}

function formatPricePerSquareMeter(value: number): string {
  return `${currencyFormatter.format(value)}/m²`
}

function countBy<T>(items: ReadonlyArray<T>, keyOf: (item: T) => string): ChartRow[] {
  const counts = new Map<string, number>()
  for (const item of items) {
    const key = keyOf(item)
    counts.set(key, (counts.get(key) ?? 0) + 1)
  }
  return [...counts.entries()]
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => b.value - a.value || a.label.localeCompare(b.label, 'pt-BR'))
}

function averagePriceByThickness(glasses: ReadonlyArray<GlassSummary>): ChartRow[] {
  const totals = new Map<number, { sum: number; count: number }>()
  for (const glass of glasses) {
    const total = totals.get(glass.thicknessMm) ?? { sum: 0, count: 0 }
    totals.set(glass.thicknessMm, { sum: total.sum + glass.pricePerSquareMeter, count: total.count + 1 })
  }
  return [...totals.entries()]
    .sort(([a], [b]) => a - b)
    .map(([thickness, total]) => ({ label: `${thickness} mm`, value: total.sum / total.count }))
}

function tooltipFormatter(format: (value: number) => string, name: string) {
  return (value: unknown) => [format(Number(value)), name] as [string, string]
}

const tooltipStyle = {
  border: '1px solid #d9d0ff',
  borderRadius: 10,
  boxShadow: '0 8px 24px rgb(91 75 240 / 15%)',
  fontSize: '0.85rem',
}

export function AnalyticsPage() {
  const { user } = useAuth()
  const canReadGlasses = user?.permissions.includes('GLASS_READ') ?? false
  const canReadKits = user?.permissions.includes('KIT_READ') ?? false
  const [inventory, setInventory] = useState<ReadonlyArray<GlassStockSummary>>([])
  const [glasses, setGlasses] = useState<ReadonlyArray<GlassSummary>>([])
  const [kits, setKits] = useState<ReadonlyArray<KitSummary>>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!canReadGlasses) return
    let current = true
    const kitsRequest = canReadKits ? listKits() : Promise.resolve({ content: [] })
    void Promise.all([listInventory(), listGlasses(), kitsRequest])
      .then(([inventoryPage, glassesPage, kitsPage]) => {
        if (!current) return
        setInventory(inventoryPage.content)
        setGlasses(glassesPage.content)
        setKits(kitsPage.content)
      })
      .catch(() => {
        if (current) setError('Não foi possível carregar os indicadores.')
      })
      .finally(() => {
        if (current) setLoading(false)
      })
    return () => {
      current = false
    }
  }, [canReadGlasses, canReadKits])

  const areaByGlass = useMemo(() => inventory
    .filter((item) => item.availableAreaM2 > 0)
    .sort((a, b) => b.availableAreaM2 - a.availableAreaM2)
    .slice(0, MAX_GLASSES_IN_AREA_CHART)
    .map((item) => ({ label: item.glassName, value: item.availableAreaM2 })), [inventory])

  const stockByStatus = useMemo(() => STATUS_ORDER
    .map((status) => ({
      status,
      label: statusLabels[status],
      value: inventory.filter((item) => item.status === status).length,
    }))
    .filter((row) => row.value > 0), [inventory])

  const activeGlasses = useMemo(() => glasses.filter((glass) => glass.active), [glasses])
  const glassesByFinish = useMemo(() => countBy(activeGlasses, (glass) => glass.finish), [activeGlasses])
  const priceByThickness = useMemo(() => averagePriceByThickness(activeGlasses), [activeGlasses])

  const totalArea = inventory.reduce((sum, item) => sum + item.availableAreaM2, 0)
  const lowCount = inventory.filter((item) => item.status === 'LOW').length

  if (!canReadGlasses) {
    return <AppShell><section className="workflow-card"><h1>Acesso não permitido</h1><p>Você não possui permissão para consultar os indicadores.</p></section></AppShell>
  }

  return (
    <AppShell>
      <section className="workflow-card">
        <div className="workflow-actions">
          <div>
            <p className="eyebrow">Gestão</p>
            <h1>Indicadores</h1>
            <p>Visão geral do estoque de chapas, do catálogo de vidros e dos kits cadastrados.</p>
          </div>
        </div>

        {error ? <p className="workflow-feedback workflow-error" role="alert">{error}</p> : null}
        {loading ? <p>Carregando indicadores...</p> : null}

        {!loading && !error ? (
          <>
            <div className="analytics-metrics" aria-label="Resumo dos indicadores">
              <div className="inventory-metric"><span className="inventory-metric__label">Área disponível</span><strong className="inventory-metric__value">{formatArea(totalArea)}</strong></div>
              <div className="inventory-metric"><span className="inventory-metric__label">Vidros ativos</span><strong className="inventory-metric__value">{activeGlasses.length}</strong></div>
              <div className="inventory-metric"><span className="inventory-metric__label">Estoque baixo</span><strong className="inventory-metric__value">{lowCount}</strong></div>
              {canReadKits ? <div className="inventory-metric"><span className="inventory-metric__label">Kits cadastrados</span><strong className="inventory-metric__value">{kits.length}</strong></div> : null}
            </div>

            <div className="analytics-grid">
              <ChartCard
                title="Área disponível por vidro"
                description={`Os ${MAX_GLASSES_IN_AREA_CHART} vidros com maior saldo em chapas.`}
                rows={areaByGlass}
                valueLabel="Área disponível"
                formatValue={formatArea}
                emptyMessage="Nenhuma chapa registrada no estoque."
              >
                <ResponsiveContainer width="100%" height={Math.max(160, areaByGlass.length * 36)}>
                  <BarChart data={areaByGlass} layout="vertical" margin={{ top: 4, right: 16, bottom: 4, left: 8 }}>
                    <BarGradient id="bar-area" colors={chartGradients.area} />
                    <CartesianGrid horizontal={false} stroke={GRID_COLOR} />
                    <XAxis type="number" tick={{ fill: AXIS_COLOR, fontSize: 12 }} tickFormatter={(value: number) => areaFormatter.format(value)} />
                    <YAxis type="category" dataKey="label" width={150} tick={{ fill: AXIS_COLOR, fontSize: 12 }} />
                    <Tooltip cursor={{ fill: CURSOR_FILL }} contentStyle={tooltipStyle} formatter={tooltipFormatter(formatArea, 'Área disponível')} />
                    <Bar dataKey="value" fill="url(#bar-area)" barSize={18} radius={[0, 8, 8, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </ChartCard>

              <ChartCard
                title="Situação do estoque"
                description="Quantidade de vidros em cada situação de estoque."
                rows={stockByStatus}
                valueLabel="Vidros"
                formatValue={formatCount}
                emptyMessage="Nenhum vidro com estoque cadastrado."
              >
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={stockByStatus} margin={{ top: 8, right: 8, bottom: 4, left: -16 }}>
                    <CartesianGrid vertical={false} stroke={GRID_COLOR} />
                    <XAxis dataKey="label" tick={{ fill: AXIS_COLOR, fontSize: 12 }} interval={0} />
                    <YAxis allowDecimals={false} tick={{ fill: AXIS_COLOR, fontSize: 12 }} />
                    <Tooltip cursor={{ fill: CURSOR_FILL }} contentStyle={tooltipStyle} formatter={tooltipFormatter(formatCount, 'Vidros')} />
                    <Bar dataKey="value" barSize={40} radius={[8, 8, 0, 0]}>
                      {stockByStatus.map((row) => <Cell key={row.status} fill={statusColors[row.status]} />)}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </ChartCard>

              <ChartCard
                title="Vidros por acabamento"
                description="Distribuição dos vidros ativos do catálogo."
                rows={glassesByFinish}
                valueLabel="Vidros"
                formatValue={formatCount}
                emptyMessage="Nenhum vidro ativo no catálogo."
              >
                <ResponsiveContainer width="100%" height={Math.max(160, glassesByFinish.length * 36)}>
                  <BarChart data={glassesByFinish} layout="vertical" margin={{ top: 4, right: 16, bottom: 4, left: 8 }}>
                    <BarGradient id="bar-finish" colors={chartGradients.finish} />
                    <CartesianGrid horizontal={false} stroke={GRID_COLOR} />
                    <XAxis type="number" allowDecimals={false} tick={{ fill: AXIS_COLOR, fontSize: 12 }} />
                    <YAxis type="category" dataKey="label" width={120} tick={{ fill: AXIS_COLOR, fontSize: 12 }} />
                    <Tooltip cursor={{ fill: CURSOR_FILL }} contentStyle={tooltipStyle} formatter={tooltipFormatter(formatCount, 'Vidros')} />
                    <Bar dataKey="value" fill="url(#bar-finish)" barSize={18} radius={[0, 8, 8, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </ChartCard>

              <ChartCard
                title="Preço médio por espessura"
                description="Média do preço por m² dos vidros ativos."
                rows={priceByThickness}
                valueLabel="Preço médio"
                formatValue={formatPricePerSquareMeter}
                emptyMessage="Nenhum vidro ativo no catálogo."
              >
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={priceByThickness} margin={{ top: 8, right: 8, bottom: 4, left: 8 }}>
                    <BarGradient id="bar-price" colors={chartGradients.price} vertical />
                    <CartesianGrid vertical={false} stroke={GRID_COLOR} />
                    <XAxis dataKey="label" tick={{ fill: AXIS_COLOR, fontSize: 12 }} interval={0} />
                    <YAxis tick={{ fill: AXIS_COLOR, fontSize: 12 }} tickFormatter={(value: number) => currencyFormatter.format(value)} width={84} />
                    <Tooltip cursor={{ fill: CURSOR_FILL }} contentStyle={tooltipStyle} formatter={tooltipFormatter(formatPricePerSquareMeter, 'Preço médio')} />
                    <Bar dataKey="value" fill="url(#bar-price)" barSize={32} radius={[8, 8, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </ChartCard>
            </div>
          </>
        ) : null}
      </section>
    </AppShell>
  )
}
