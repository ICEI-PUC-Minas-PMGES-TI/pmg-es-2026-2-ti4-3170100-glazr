import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, ClipboardPenLine, PackagePlus, Scissors, SlidersHorizontal } from 'lucide-react'
import { useAuth } from '../features/auth/hooks/use-auth'
import {
  adjustSheet,
  cutSheet,
  getStockSummary,
  listGlassSheets,
  listStockMovements,
  receiveSheets,
  updateStockMinimum,
  type GlassSheet,
  type GlassStockSummary,
  type StockMovement,
  type StockStatus,
} from '../features/inventory/api/inventory-api'
import { ApiError } from '../shared/api/api-error'
import { AppShell } from '../shared/components/AppShell'
import './WorkflowPages.css'
import './InventoryPages.css'

const areaFormatter = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const dateFormatter = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })

function statusLabel(status: StockStatus): string {
  if (status === 'LOW') return 'Estoque baixo'
  if (status === 'UNCONFIGURED') return 'Mínimo não configurado'
  return 'Regular'
}

function statusClass(status: StockStatus): string {
  if (status === 'LOW') return 'badge--low'
  if (status === 'UNCONFIGURED') return 'badge--unconfigured'
  return 'badge--active'
}

function movementLabel(movement: StockMovement): string {
  if (movement.movementType === 'RECEIPT') return 'Recebimento'
  if (movement.movementType === 'ADJUSTMENT') return 'Ajuste manual'
  return 'Corte manual'
}

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

function parseNumber(value: string, field: string, minimum = 0): number {
  const parsed = Number.parseFloat(value.replace(',', '.'))
  if (!Number.isFinite(parsed) || parsed < minimum) throw new Error(`${field} inválido.`)
  return parsed
}

export function InventoryGlassPage() {
  const { id } = useParams<{ id: string }>()
  const { user } = useAuth()
  const [summary, setSummary] = useState<GlassStockSummary | null>(null)
  const [sheets, setSheets] = useState<ReadonlyArray<GlassSheet>>([])
  const [movements, setMovements] = useState<ReadonlyArray<StockMovement>>([])
  const [selectedSheetId, setSelectedSheetId] = useState('')
  const [minimum, setMinimum] = useState('')
  const [receiptWidth, setReceiptWidth] = useState('2000')
  const [receiptHeight, setReceiptHeight] = useState('3000')
  const [receiptQuantity, setReceiptQuantity] = useState('1')
  const [receiptNotes, setReceiptNotes] = useState('')
  const [adjustedArea, setAdjustedArea] = useState('')
  const [adjustmentNotes, setAdjustmentNotes] = useState('')
  const [pieceWidth, setPieceWidth] = useState('')
  const [pieceHeight, setPieceHeight] = useState('')
  const [usableRemainder, setUsableRemainder] = useState('')
  const [cutNotes, setCutNotes] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const canManage = user?.permissions.includes('GLASS_UPDATE') ?? false

  const load = useCallback(async (): Promise<void> => {
    if (!id) return
    try {
      const [stock, sheetsPage, movementsPage] = await Promise.all([
        getStockSummary(id), listGlassSheets(id), listStockMovements(id),
      ])
      setSummary(stock)
      setMinimum(stock.minimumAreaM2 === null ? '' : String(stock.minimumAreaM2))
      setSheets(sheetsPage.content)
      setMovements(movementsPage.content)
      setSelectedSheetId((current) => sheetsPage.content.some(
        (sheet) => sheet.id === current && sheet.status === 'AVAILABLE',
      ) ? current : sheetsPage.content.find((sheet) => sheet.status === 'AVAILABLE')?.id || '')
    } catch (requestError: unknown) {
      setError(errorMessage(requestError, 'Não foi possível carregar o detalhe do estoque.'))
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    if (!id) return
    let current = true
    void Promise.all([getStockSummary(id), listGlassSheets(id), listStockMovements(id)])
      .then(([stock, sheetsPage, movementsPage]) => {
        if (!current) return
        setSummary(stock)
        setMinimum(stock.minimumAreaM2 === null ? '' : String(stock.minimumAreaM2))
        setSheets(sheetsPage.content)
        setMovements(movementsPage.content)
        setSelectedSheetId((selected) => sheetsPage.content.some(
          (sheet) => sheet.id === selected && sheet.status === 'AVAILABLE',
        ) ? selected : sheetsPage.content.find((sheet) => sheet.status === 'AVAILABLE')?.id || '')
      })
      .catch((requestError: unknown) => {
        if (current) setError(errorMessage(requestError, 'Não foi possível carregar o detalhe do estoque.'))
      })
      .finally(() => {
        if (current) setLoading(false)
      })
    return () => {
      current = false
    }
  }, [id])

  const selectedSheet = useMemo(
    () => sheets.find((sheet) => sheet.id === selectedSheetId) ?? null,
    [sheets, selectedSheetId],
  )

  async function run(action: () => Promise<string>): Promise<void> {
    setBusy(true)
    setError(null)
    setSuccess(null)
    try {
      setSuccess(await action())
      await load()
    } catch (requestError: unknown) {
      setError(errorMessage(requestError, 'Não foi possível concluir a movimentação de estoque.'))
    } finally {
      setBusy(false)
    }
  }

  function submitMinimum(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    if (!id) return
    void run(async () => {
      const value = minimum.trim() ? parseNumber(minimum, 'Estoque mínimo') : null
      await updateStockMinimum(id, value)
      return value === null ? 'Estoque mínimo removido.' : 'Estoque mínimo atualizado.'
    })
  }

  function submitReceipt(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    if (!id) return
    void run(async () => {
      const quantity = parseNumber(receiptQuantity, 'Quantidade', 1)
      if (!Number.isInteger(quantity)) throw new Error('Quantidade deve ser um número inteiro.')
      await receiveSheets(id, {
        widthMm: parseNumber(receiptWidth, 'Largura', 0.01),
        heightMm: parseNumber(receiptHeight, 'Altura', 0.01),
        quantity,
        notes: receiptNotes.trim() || null,
      })
      setReceiptNotes('')
      return 'Chapas recebidas e adicionadas ao estoque.'
    })
  }

  function submitAdjustment(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    if (!selectedSheet) {
      setError('Selecione uma chapa para ajustar.')
      return
    }
    void run(async () => {
      await adjustSheet(selectedSheet.id, {
        remainingAreaM2: parseNumber(adjustedArea, 'Saldo disponível'),
        notes: adjustmentNotes.trim(),
      })
      setAdjustedArea('')
      setAdjustmentNotes('')
      return 'Saldo da chapa ajustado e registrado no histórico.'
    })
  }

  function submitCut(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    if (!selectedSheet) {
      setError('Selecione uma chapa para registrar o corte.')
      return
    }
    void run(async () => {
      const result = await cutSheet(selectedSheet.id, {
        pieceWidthMm: parseNumber(pieceWidth, 'Largura da peça', 0.01),
        pieceHeightMm: parseNumber(pieceHeight, 'Altura da peça', 0.01),
        usableRemainderAreaM2: parseNumber(usableRemainder, 'Sobra útil'),
        notes: cutNotes.trim() || null,
      })
      setPieceWidth('')
      setPieceHeight('')
      setUsableRemainder('')
      setCutNotes('')
      return `Corte registrado: aproveitamento de ${areaFormatter.format(result.yieldPercent)}%.`
    })
  }

  if (!user?.permissions.includes('GLASS_READ')) {
    return <AppShell><section className="workflow-card"><h1>Acesso não permitido</h1><p>Você não possui permissão para consultar o estoque.</p></section></AppShell>
  }

  if (loading) {
    return <AppShell><section className="workflow-card"><p>Carregando estoque...</p></section></AppShell>
  }

  if (!summary) {
    return <AppShell><section className="workflow-card"><h1>Estoque não encontrado</h1><p>O vidro não existe ou não pode ser consultado.</p><Link className="button button--secondary" to="/inventory">Voltar ao estoque</Link></section></AppShell>
  }

  return (
    <AppShell>
      <section className="workflow-card">
        <Link className="button button--secondary" to="/inventory"><ArrowLeft size={15} /> Voltar ao estoque</Link>
        <div className="inventory-detail__heading">
          <div>
            <p className="eyebrow">Estoque do vidro</p>
            <h1>{summary.glassName}</h1>
            <p className="inventory-detail__specification">{summary.thicknessMm} mm · {summary.finish} · {summary.color}</p>
          </div>
          <span className={`badge ${statusClass(summary.status)}`}>{statusLabel(summary.status)}</span>
        </div>

        <div className="inventory-metrics" aria-label="Resumo da chapa">
          <div className="inventory-metric"><span className="inventory-metric__label">Saldo disponível</span><strong className="inventory-metric__value">{areaFormatter.format(summary.availableAreaM2)} m²</strong></div>
          <div className="inventory-metric"><span className="inventory-metric__label">Estoque mínimo</span><strong className="inventory-metric__value">{summary.minimumAreaM2 === null ? '—' : `${areaFormatter.format(summary.minimumAreaM2)} m²`}</strong></div>
          <div className="inventory-metric"><span className="inventory-metric__label">Chapas rastreadas</span><strong className="inventory-metric__value">{sheets.length}</strong></div>
        </div>

        {error ? <p className="workflow-feedback workflow-error" role="alert">{error}</p> : null}
        {success ? <p className="workflow-feedback" role="status">{success}</p> : null}

        {canManage ? <div className="inventory-actions-grid">
          <section className="inventory-action-panel"><SlidersHorizontal size={18} aria-hidden="true" /><h2>Estoque mínimo</h2><form className="workflow-form" onSubmit={submitMinimum}><label>Limite em m²<input aria-label="Estoque mínimo em metros quadrados" type="number" min="0" step="0.000001" value={minimum} onChange={(event) => setMinimum(event.target.value)} disabled={busy} placeholder="Não configurado" /></label><button className="button button--secondary" disabled={busy} type="submit">Salvar mínimo</button></form></section>
          <section className="inventory-action-panel"><PackagePlus size={18} aria-hidden="true" /><h2>Receber chapas</h2><form className="workflow-form" onSubmit={submitReceipt}><div className="inventory-field-grid"><label>Largura (mm)<input aria-label="Largura da chapa" type="number" min="0.01" step="0.01" required value={receiptWidth} onChange={(event) => setReceiptWidth(event.target.value)} disabled={busy} /></label><label>Altura (mm)<input aria-label="Altura da chapa" type="number" min="0.01" step="0.01" required value={receiptHeight} onChange={(event) => setReceiptHeight(event.target.value)} disabled={busy} /></label></div><label>Quantidade<input aria-label="Quantidade de chapas" type="number" min="1" step="1" required value={receiptQuantity} onChange={(event) => setReceiptQuantity(event.target.value)} disabled={busy} /></label><label>Observação<input aria-label="Observação do recebimento" maxLength={300} value={receiptNotes} onChange={(event) => setReceiptNotes(event.target.value)} disabled={busy} /></label><button className="button button--primary" disabled={busy} type="submit">Registrar recebimento</button></form></section>
          <section className="inventory-action-panel"><ClipboardPenLine size={18} aria-hidden="true" /><h2>Ajustar saldo</h2><form className="workflow-form" onSubmit={submitAdjustment}><label>Chapa<select aria-label="Chapa para ajuste" value={selectedSheetId} onChange={(event) => setSelectedSheetId(event.target.value)} disabled={busy || sheets.length === 0}>{sheets.filter((sheet) => sheet.status === 'AVAILABLE').map((sheet) => <option value={sheet.id} key={sheet.id}>{areaFormatter.format(sheet.remainingAreaM2)} m² disponíveis</option>)}</select></label><label>Novo saldo (m²)<input aria-label="Novo saldo da chapa" type="number" min="0" step="0.000001" required value={adjustedArea} onChange={(event) => setAdjustedArea(event.target.value)} disabled={busy || !selectedSheet} /></label><label>Motivo<input aria-label="Motivo do ajuste" maxLength={300} required value={adjustmentNotes} onChange={(event) => setAdjustmentNotes(event.target.value)} disabled={busy || !selectedSheet} /></label><button className="button button--secondary" disabled={busy || !selectedSheet} type="submit">Registrar ajuste</button></form></section>
          <section className="inventory-action-panel"><Scissors size={18} aria-hidden="true" /><h2>Registrar corte</h2><form className="workflow-form" onSubmit={submitCut}><label>Chapa<select aria-label="Chapa para corte" value={selectedSheetId} onChange={(event) => setSelectedSheetId(event.target.value)} disabled={busy || sheets.length === 0}>{sheets.filter((sheet) => sheet.status === 'AVAILABLE').map((sheet) => <option value={sheet.id} key={sheet.id}>{areaFormatter.format(sheet.remainingAreaM2)} m² disponíveis</option>)}</select></label><div className="inventory-field-grid"><label>Largura da peça (mm)<input aria-label="Largura da peça" type="number" min="0.01" step="0.01" required value={pieceWidth} onChange={(event) => setPieceWidth(event.target.value)} disabled={busy || !selectedSheet} /></label><label>Altura da peça (mm)<input aria-label="Altura da peça" type="number" min="0.01" step="0.01" required value={pieceHeight} onChange={(event) => setPieceHeight(event.target.value)} disabled={busy || !selectedSheet} /></label></div><label>Sobra útil (m²)<input aria-label="Sobra útil em metros quadrados" type="number" min="0" step="0.000001" required value={usableRemainder} onChange={(event) => setUsableRemainder(event.target.value)} disabled={busy || !selectedSheet} /></label><label>Observação<input aria-label="Observação do corte" maxLength={300} value={cutNotes} onChange={(event) => setCutNotes(event.target.value)} disabled={busy || !selectedSheet} /></label><button className="button button--primary" disabled={busy || !selectedSheet} type="submit">Registrar corte</button></form></section>
        </div> : null}

        <section className="inventory-section"><div className="inventory-section__header"><h2>Chapas</h2><span>{sheets.length} registrada(s)</span></div>{sheets.length === 0 ? <p className="inventory-empty">Nenhuma chapa foi recebida para este vidro.</p> : <table className="data-table"><thead><tr><th>Dimensões</th><th>Área original</th><th>Saldo</th><th>Status</th></tr></thead><tbody>{sheets.map((sheet) => <tr className={sheet.id === selectedSheetId ? 'inventory-row-selected' : ''} key={sheet.id}><td>{sheet.originalWidthMm} × {sheet.originalHeightMm} mm</td><td>{areaFormatter.format(sheet.originalAreaM2)} m²</td><td>{areaFormatter.format(sheet.remainingAreaM2)} m²</td><td><span className={`badge ${sheet.status === 'AVAILABLE' ? 'badge--active' : 'badge--inactive'}`}>{sheet.status === 'AVAILABLE' ? 'Disponível' : 'Esgotada'}</span></td></tr>)}</tbody></table>}</section>

        <section className="inventory-section"><div className="inventory-section__header"><h2>Histórico</h2><span>{movements.length} movimentação(ões)</span></div>{movements.length === 0 ? <p className="inventory-empty">As movimentações aparecerão aqui após o primeiro recebimento.</p> : <table className="data-table"><thead><tr><th>Operação</th><th>Peça / perda</th><th>Saldo resultante</th><th>Quando</th></tr></thead><tbody>{movements.map((movement) => <tr key={movement.id}><td><strong>{movementLabel(movement)}</strong><br />{movement.notes ? <span>{movement.notes}</span> : null}</td><td>{movement.pieceAreaM2 === null ? '—' : `${areaFormatter.format(movement.pieceAreaM2)} m²`} {movement.lossAreaM2 === null ? '' : `· perda ${areaFormatter.format(movement.lossAreaM2)} m²`}</td><td>{areaFormatter.format(movement.resultingRemainingAreaM2)} m²</td><td>{dateFormatter.format(new Date(movement.createdAt))}</td></tr>)}</tbody></table>}</section>
      </section>
    </AppShell>
  )
}
