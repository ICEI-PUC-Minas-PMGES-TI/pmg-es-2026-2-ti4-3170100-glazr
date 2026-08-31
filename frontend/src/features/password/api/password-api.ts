import { csrfRequest } from '../../../shared/api/csrf-client'

export const requestPasswordReset = (email: string) => csrfRequest<void>('/api/v1/auth/password-reset/request', {
  method: 'POST', body: JSON.stringify({ email }),
})

export const completePasswordReset = (token: string, newPassword: string) =>
  csrfRequest<void>('/api/v1/auth/password-reset/complete', {
    method: 'POST', body: JSON.stringify({ token, newPassword }),
  })

export const changePassword = (currentPassword: string, newPassword: string) =>
  csrfRequest<void>('/api/v1/auth/password/change', {
    method: 'POST', body: JSON.stringify({ currentPassword, newPassword }),
  })
