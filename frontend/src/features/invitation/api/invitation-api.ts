import { csrfRequest } from '../../../shared/api/csrf-client'
import type { AuthenticatedUser } from '../../auth/model/auth-types'

export interface InvitationInspection {
  displayName: string
  email: string
  expiresAt: string
  availableMethods: Array<'PASSWORD' | 'GOOGLE'>
}

export const inspectInvitation = (token: string) =>
  csrfRequest<InvitationInspection>('/api/v1/invitations/inspect', {
    method: 'POST', body: JSON.stringify({ token }),
  })

export const acceptInvitation = (token: string, password: string) =>
  csrfRequest<AuthenticatedUser>('/api/v1/invitations/accept-with-password', {
    method: 'POST', body: JSON.stringify({ token, password }),
  })

export async function startInvitationGoogle(token: string): Promise<void> {
  const result = await csrfRequest<{ authorizationUrl: string }>('/api/v1/invitations/google/start', {
    method: 'POST', body: JSON.stringify({ token }),
  })
  window.location.assign(result.authorizationUrl)
}
