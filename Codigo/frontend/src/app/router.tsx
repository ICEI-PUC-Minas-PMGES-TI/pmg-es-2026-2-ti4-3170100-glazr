import { Navigate, Route, Routes } from 'react-router-dom'

import { LandingPage } from '../pages/LandingPage'
import { HomePage } from '../pages/HomePage'
import { LoginPage } from '../pages/LoginPage'
import { ActivatePage } from '../pages/ActivatePage'
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage'
import { ResetPasswordPage } from '../pages/ResetPasswordPage'
import { AccountSecurityPage } from '../pages/AccountSecurityPage'
import { AdminUsersPage } from '../pages/AdminUsersPage'
import { NewAdminUserPage } from '../pages/NewAdminUserPage'
import { EditAdminUserPage } from '../pages/EditAdminUserPage'
import { AdminKitsPage } from '../pages/AdminKitsPage'
import { NewAdminKitPage } from '../pages/NewAdminKitPage'
import { GlassesPage } from '../pages/GlassesPage'
import { NewGlassPage } from '../pages/NewGlassPage'
import { EditGlassPage } from '../pages/EditGlassPage'
import { ProtectedRoute } from './ProtectedRoute'
import { PublicOnlyRoute } from './PublicOnlyRoute'

export function AppRoutes() {
  return (
    <Routes>
      {/* Public landing page */}
      <Route path="/" element={<LandingPage />} />

      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>
      <Route path="/activate" element={<ActivatePage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<HomePage />} />
        <Route path="/account/security" element={<AccountSecurityPage />} />
        <Route path="/admin/users" element={<AdminUsersPage />} />
        <Route path="/admin/users/new" element={<NewAdminUserPage />} />
        <Route path="/admin/users/:id/edit" element={<EditAdminUserPage />} />
        <Route path="/admin/kits" element={<AdminKitsPage />} />
        <Route path="/admin/kits/new" element={<NewAdminKitPage />} />
        <Route path="/glasses" element={<GlassesPage />} />
        <Route path="/glasses/new" element={<NewGlassPage />} />
        <Route path="/glasses/:id/edit" element={<EditGlassPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
