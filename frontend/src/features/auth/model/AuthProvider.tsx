import { useCallback, useEffect, useMemo, useState, type PropsWithChildren } from 'react'

import { ApiError } from '../../../shared/api/api-error'
import * as authApi from '../api/auth-api'
import { AuthContext } from './auth-context'
import type { AuthenticatedUser, AuthStatus, LoginCredentials } from './auth-types'

const INVALID_CREDENTIALS_MESSAGE = 'E-mail ou senha inválidos.'
const NETWORK_MESSAGE = 'Não foi possível conectar ao servidor. Tente novamente.'
const SESSION_CHECK_MESSAGE = 'Não foi possível verificar sua sessão. Você ainda pode tentar entrar.'
const UNEXPECTED_MESSAGE = 'Não foi possível concluir a operação. Tente novamente.'

function authenticationMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 0) return NETWORK_MESSAGE
    if (error.code === 'AUTHENTICATION_FAILED') return INVALID_CREDENTIALS_MESSAGE
  }
  return UNEXPECTED_MESSAGE
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true

    void authApi
      .getCurrentUser()
      .then((authenticatedUser) => {
        if (!active) return
        setUser(authenticatedUser)
        setStatus('authenticated')
      })
      .catch((requestError: unknown) => {
        if (!active) return
        setUser(null)
        setStatus('unauthenticated')
        if (!(requestError instanceof ApiError && requestError.status === 401)) {
          setError(
            requestError instanceof ApiError && requestError.status === 0
              ? SESSION_CHECK_MESSAGE
              : UNEXPECTED_MESSAGE,
          )
        }
      })

    return () => {
      active = false
    }
  }, [])

  const clearError = useCallback(() => setError(null), [])

  const handleLogin = useCallback(async (credentials: LoginCredentials) => {
    setIsSubmitting(true)
    setError(null)
    try {
      const authenticatedUser = await authApi.login(credentials)
      setUser(authenticatedUser)
      setStatus('authenticated')
      return true
    } catch (requestError) {
      setUser(null)
      setStatus('unauthenticated')
      setError(authenticationMessage(requestError))
      return false
    } finally {
      setIsSubmitting(false)
    }
  }, [])

  const handleLogout = useCallback(async () => {
    setIsSubmitting(true)
    setError(null)
    try {
      await authApi.logout()
    } catch (requestError) {
      if (!(requestError instanceof ApiError && [401, 403].includes(requestError.status))) {
        setError(authenticationMessage(requestError))
      }
    } finally {
      setUser(null)
      setStatus('unauthenticated')
      setIsSubmitting(false)
    }
  }, [])

  const value = useMemo(
    () => ({
      status,
      user,
      isSubmitting,
      error,
      login: handleLogin,
      logout: handleLogout,
      clearError,
    }),
    [clearError, error, handleLogin, handleLogout, isSubmitting, status, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
