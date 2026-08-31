export interface AuthenticatedUser {
  id: string
  email: string
  displayName: string
  active: boolean
  profiles: string[]
  permissions: string[]
}

export interface LoginCredentials {
  email: string
  password: string
}

export interface CsrfResponse {
  headerName: string
  parameterName: string
  token: string
}

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'
