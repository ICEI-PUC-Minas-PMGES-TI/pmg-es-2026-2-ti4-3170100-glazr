import { UserSummary } from '../features/auth/components/UserSummary'
import { AppShell } from '../shared/components/AppShell'
import './HomePage.css'

export function HomePage() {
  return (
    <AppShell>
      <UserSummary />
    </AppShell>
  )
}
