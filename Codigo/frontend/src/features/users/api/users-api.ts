import { csrfRequest } from '../../../shared/api/csrf-client'
import { requestJson } from '../../../shared/api/http-client'

export interface UserSummary {
  id: string; email: string; displayName: string; active: boolean; accountType: string
  authenticationMethods: string[]; invitationStatus: string | null; invitationExpiresAt: string | null
  invitationDeliveryStatus: string | null; accessProfiles: Array<{ id: string; name: string }>
}
export interface AccessProfile { id: string; name: string; active: boolean }
interface Page<T> { content: T[] }

export const listUsers = () => requestJson<Page<UserSummary>>('/api/v1/users?size=100&sort=displayName,asc')
export const listProfiles = () => requestJson<Page<AccessProfile>>('/api/v1/access-profiles?size=100&sort=name,asc')
export const createUser = (body: { email: string; displayName: string; accessProfileIds: string[] }) =>
  csrfRequest<UserSummary>('/api/v1/users', { method: 'POST', body: JSON.stringify(body) })
export const resendInvitation = (id: string) => csrfRequest(`/api/v1/users/${id}/invitation/resend`, { method: 'POST' })
export const revokeInvitation = (id: string) => csrfRequest(`/api/v1/users/${id}/invitation/revoke`, { method: 'POST' })
export const updateUserStatus = (id: string, active: boolean) => csrfRequest(`/api/v1/users/${id}/status`, {
  method: 'PATCH', body: JSON.stringify({ active }),
})
