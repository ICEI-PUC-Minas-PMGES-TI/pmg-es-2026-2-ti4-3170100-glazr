import { Link } from 'react-router-dom'

import { UserSummary } from '../features/auth/components/UserSummary'
import './HomePage.css'

export function HomePage() {
  return (
    <main className="home-page">
      <header className="home-header">
        <Link className="brand brand--compact" to="/" aria-label="Glazr — página inicial">
          <span className="brand__mark" aria-hidden="true">
            G
          </span>
          <span>Glazr</span>
        </Link>
        <span className="home-header__status">
          <span aria-hidden="true" /> Ambiente seguro
        </span>
      </header>
      <UserSummary />
    </main>
  )
}
