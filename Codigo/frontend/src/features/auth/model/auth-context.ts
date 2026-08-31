import { createContext } from 'react'

import type { AuthenticatedUser, AuthStatus, LoginCredentials } from './auth-types'

export interface AuthContextValue {
  status: AuthStatus
  user: AuthenticatedUser | null
  isSubmitting: boolean
  error: string | null
  login: (credentials: LoginCredentials) => Promise<boolean>
  logout: () => Promise<void>
  clearError: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
