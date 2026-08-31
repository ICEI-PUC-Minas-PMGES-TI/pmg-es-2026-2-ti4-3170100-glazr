import { Navigate, Outlet } from 'react-router-dom'

import { useAuth } from '../features/auth/hooks/use-auth'
import { AppLoading } from '../shared/components/AppLoading'

export function PublicOnlyRoute() {
  const { status } = useAuth()

  if (status === 'loading') return <AppLoading />
  if (status === 'authenticated') return <Navigate to="/dashboard" replace />
  return <Outlet />
}
