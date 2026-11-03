import { csrfRequest } from '../../../shared/api/csrf-client'
import { requestJson } from '../../../shared/api/http-client'
import type { Page } from '../../../shared/api/page'

export interface KitComponent {
  name: string; quantity: number; notes: string | null
}
export interface KitSummary {
  id: string; name: string; color: string; price: number; components: KitComponent[]
  createdAt: string; updatedAt: string
}

export interface KitInput {
  name: string; color: string; price: number; components: KitComponent[]
}

export const listKits = () => requestJson<Page<KitSummary>>('/api/v1/kits?size=100&sort=name,asc')
export const getKit = (id: string) => requestJson<KitSummary>(`/api/v1/kits/${id}`)
export const createKit = (body: KitInput) =>
  csrfRequest<KitSummary>('/api/v1/kits', { method: 'POST', body: JSON.stringify(body) })
export const updateKit = (id: string, body: KitInput) =>
  csrfRequest<KitSummary>(`/api/v1/kits/${id}`, { method: 'PUT', body: JSON.stringify(body) })
export const deleteKit = (id: string) => csrfRequest(`/api/v1/kits/${id}`, { method: 'DELETE' })