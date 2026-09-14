import { csrfRequest } from '../../../shared/api/csrf-client'
import { requestJson } from '../../../shared/api/http-client'

export interface KitComponent {
  name: string; quantity: number; notes: string | null
}
export interface KitSummary {
  id: string; name: string; color: string; price: number; components: KitComponent[]
  createdAt: string; updatedAt: string
}
interface Page<T> { content: T[] }

export const listKits = () => requestJson<Page<KitSummary>>('/api/v1/kits?size=100&sort=name,asc')
export const createKit = (body: { name: string; color: string; price: number; components: KitComponent[] }) =>
  csrfRequest<KitSummary>('/api/v1/kits', { method: 'POST', body: JSON.stringify(body) })
export const deleteKit = (id: string) => csrfRequest(`/api/v1/kits/${id}`, { method: 'DELETE' })