import { requestJson } from '../../../shared/api/http-client'
import type { AuthenticatedUser, CsrfResponse, LoginCredentials } from '../model/auth-types'

const AUTH_BASE_PATH = '/api/v1/auth'

async function getCsrfToken(): Promise<CsrfResponse> {
  return requestJson<CsrfResponse>(`${AUTH_BASE_PATH}/csrf`)
}

export function getCurrentUser(): Promise<AuthenticatedUser> {
  return requestJson<AuthenticatedUser>(`${AUTH_BASE_PATH}/me`)
}

export async function login(credentials: LoginCredentials): Promise<AuthenticatedUser> {
  const csrf = await getCsrfToken()
  return requestJson<AuthenticatedUser>(`${AUTH_BASE_PATH}/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(credentials),
  })
}

export async function logout(): Promise<void> {
  const csrf = await getCsrfToken()
  await requestJson<void>(`${AUTH_BASE_PATH}/logout`, {
    method: 'POST',
    headers: { [csrf.headerName]: csrf.token },
  })
}
