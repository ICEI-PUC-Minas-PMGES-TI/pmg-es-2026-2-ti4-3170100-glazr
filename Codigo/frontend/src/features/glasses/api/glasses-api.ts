import { csrfRequest } from '../../../shared/api/csrf-client'
import { requestJson } from '../../../shared/api/http-client'

export interface GlassSummary {
  id: string
  name: string
  thicknessMm: number
  finish: string
  color: string
  pricePerSquareMeter: number
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateGlassPayload {
  name: string
  thicknessMm: number
  finish: string
  color: string
  pricePerSquareMeter: number
}

export interface UpdateGlassPayload {
  name: string
  thicknessMm: number
  finish: string
  color: string
  pricePerSquareMeter: number
}

interface Page<T> {
  content: T[]
  totalElements?: number
}

export const listGlasses = (params?: {
  query?: string
  thicknessMm?: number
  finish?: string
  active?: boolean
}) => {
  const queryParams = new URLSearchParams({ size: '100', sort: 'name,asc' })
  if (params?.query) queryParams.set('query', params.query)
  if (params?.thicknessMm !== undefined && params?.thicknessMm !== null) {
    queryParams.set('thicknessMm', String(params.thicknessMm))
  }
  if (params?.finish) queryParams.set('finish', params.finish)
  if (params?.active !== undefined) queryParams.set('active', String(params.active))
  return requestJson<Page<GlassSummary>>(`/api/v1/glasses?${queryParams.toString()}`)
}

export const getGlass = (id: string) => requestJson<GlassSummary>(`/api/v1/glasses/${id}`)

export const createGlass = (body: CreateGlassPayload) =>
  csrfRequest<GlassSummary>('/api/v1/glasses', { method: 'POST', body: JSON.stringify(body) })

export const updateGlass = (id: string, body: UpdateGlassPayload) =>
  csrfRequest<GlassSummary>(`/api/v1/glasses/${id}`, { method: 'PATCH', body: JSON.stringify(body) })

export const updateGlassStatus = (id: string, active: boolean) =>
  csrfRequest<GlassSummary>(`/api/v1/glasses/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ active }),
  })

export const deleteGlass = (id: string) => csrfRequest(`/api/v1/glasses/${id}`, { method: 'DELETE' })
