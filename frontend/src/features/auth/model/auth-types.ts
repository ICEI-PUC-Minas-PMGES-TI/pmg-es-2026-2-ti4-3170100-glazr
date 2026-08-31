export interface AuthenticatedUser {
  id: string
  email: string
  displayName: string
  active: boolean
  accountType: 'EMPLOYEE' | 'CUSTOMER'
  profiles: string[]
  permissions: string[]
  authenticationMethods: Array<'PASSWORD' | 'GOOGLE'>
}

export interface AuthenticationConfiguration {
  googleEnabled: boolean
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
