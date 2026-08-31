import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useAuth } from '../features/auth/hooks/use-auth'
import { AppLoading } from '../shared/components/AppLoading'

export function ProtectedRoute() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <AppLoading />
  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}
