import { requestJson } from '../../../shared/api/http-client'
import { csrfRequest } from '../../../shared/api/csrf-client'
import type { AuthenticatedUser, AuthenticationConfiguration, LoginCredentials } from '../model/auth-types'

const AUTH_BASE_PATH = '/api/v1/auth'

export function getCurrentUser(): Promise<AuthenticatedUser> {
  return requestJson<AuthenticatedUser>(`${AUTH_BASE_PATH}/me`)
}

export async function login(credentials: LoginCredentials): Promise<AuthenticatedUser> {
  return csrfRequest<AuthenticatedUser>(`${AUTH_BASE_PATH}/login`, {
    method: 'POST',
    body: JSON.stringify(credentials),
  })
}

export async function logout(): Promise<void> {
  await csrfRequest<void>(`${AUTH_BASE_PATH}/logout`, {
    method: 'POST',
  })
}

export function getAuthenticationConfiguration(): Promise<AuthenticationConfiguration> {
  return requestJson<AuthenticationConfiguration>(`${AUTH_BASE_PATH}/config`)
}

export async function startGoogle(path = `${AUTH_BASE_PATH}/google/start`, body?: object): Promise<void> {
  const response = await csrfRequest<{ authorizationUrl: string }>(path, {
    method: 'POST',
    body: body ? JSON.stringify(body) : undefined,
  })
  window.location.assign(response.authorizationUrl)
}
