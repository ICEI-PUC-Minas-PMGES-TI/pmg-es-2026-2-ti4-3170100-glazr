import { csrfRequest } from '../../../shared/api/csrf-client'
import { requestJson } from '../../../shared/api/http-client'
import type { Page } from '../../../shared/api/page'

export type StockStatus = 'OK' | 'LOW' | 'UNCONFIGURED'
export type SheetStatus = 'AVAILABLE' | 'DEPLETED'
export type StockMovementType = 'RECEIPT' | 'ADJUSTMENT' | 'MANUAL_CUT'

export interface GlassStockSummary {
  readonly glassId: string
  readonly glassName: string
  readonly thicknessMm: number
  readonly finish: string
  readonly color: string
  readonly glassActive: boolean
  readonly availableAreaM2: number
  readonly minimumAreaM2: number | null
  readonly status: StockStatus
}

export interface GlassSheet {
  readonly id: string
  readonly glassId: string
  readonly originalWidthMm: number
  readonly originalHeightMm: number
  readonly originalAreaM2: number
  readonly remainingAreaM2: number
  readonly status: SheetStatus
  readonly createdAt: string
  readonly updatedAt: string
}

export interface StockMovement {
  readonly id: string
  readonly glassSheetId: string
  readonly movementType: StockMovementType
  readonly previousRemainingAreaM2: number | null
  readonly resultingRemainingAreaM2: number
  readonly pieceWidthMm: number | null
  readonly pieceHeightMm: number | null
  readonly pieceAreaM2: number | null
  readonly lossAreaM2: number | null
  readonly notes: string | null
  readonly performedByUserId: string
  readonly createdAt: string
}

export interface ManualCutResult {
  readonly sheet: GlassSheet
  readonly movement: StockMovement
  readonly yieldPercent: number
}

export interface ReceiveSheetsPayload {
  readonly widthMm: number
  readonly heightMm: number
  readonly quantity: number
  readonly notes: string | null
}

export interface AdjustSheetPayload {
  readonly remainingAreaM2: number
  readonly notes: string
}

export interface CutSheetPayload {
  readonly pieceWidthMm: number
  readonly pieceHeightMm: number
  readonly usableRemainderAreaM2: number
  readonly notes: string | null
}

export function listInventory(): Promise<Page<GlassStockSummary>> {
  return requestJson<Page<GlassStockSummary>>('/api/v1/inventory/glasses?size=100&sort=name,asc')
}

export function listLowStockAlerts(): Promise<ReadonlyArray<GlassStockSummary>> {
  return requestJson<ReadonlyArray<GlassStockSummary>>('/api/v1/inventory/glasses/alerts')
}

export function getStockSummary(glassId: string): Promise<GlassStockSummary> {
  return requestJson<GlassStockSummary>(`/api/v1/inventory/glasses/${glassId}`)
}

export function listGlassSheets(glassId: string): Promise<Page<GlassSheet>> {
  return requestJson<Page<GlassSheet>>(`/api/v1/inventory/glasses/${glassId}/sheets?size=100&sort=createdAt,desc`)
}

export function listStockMovements(glassId: string): Promise<Page<StockMovement>> {
  return requestJson<Page<StockMovement>>(`/api/v1/inventory/glasses/${glassId}/movements?size=100&sort=createdAt,desc`)
}

export function updateStockMinimum(glassId: string, minimumAreaM2: number | null): Promise<GlassStockSummary> {
  return csrfRequest<GlassStockSummary>(`/api/v1/inventory/glasses/${glassId}/minimum`, {
    method: 'PUT',
    body: JSON.stringify({ minimumAreaM2 }),
  })
}

export function receiveSheets(glassId: string, payload: ReceiveSheetsPayload): Promise<ReadonlyArray<GlassSheet>> {
  return csrfRequest<ReadonlyArray<GlassSheet>>(`/api/v1/inventory/glasses/${glassId}/receipts`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function adjustSheet(sheetId: string, payload: AdjustSheetPayload): Promise<GlassSheet> {
  return csrfRequest<GlassSheet>(`/api/v1/inventory/sheets/${sheetId}/adjustment`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })
}

export function cutSheet(sheetId: string, payload: CutSheetPayload): Promise<ManualCutResult> {
  return csrfRequest<ManualCutResult>(`/api/v1/inventory/sheets/${sheetId}/cuts`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
