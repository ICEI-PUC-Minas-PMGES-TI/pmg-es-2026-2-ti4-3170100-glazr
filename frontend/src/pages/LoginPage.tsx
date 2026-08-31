import { Link } from 'react-router-dom'

import { LoginForm } from '../features/auth/components/LoginForm'

export function LoginPage() {
  return (
    <main className="login-page">
      <section className="login-page__intro" aria-labelledby="glazr-brand">
        <Link className="brand" to="/" id="glazr-brand" aria-label="Glazr — página inicial">
          <span className="brand__mark" aria-hidden="true">
            G
          </span>
          <span>Glazr</span>
        </Link>
        <div className="login-page__message">
          <p className="eyebrow">Gestão feita sob medida</p>
          <h2>Seu negócio mais claro, do primeiro contato à entrega.</h2>
          <p>
            Centralize a rotina da vidraçaria em um ambiente simples, seguro e preparado para crescer.
          </p>
        </div>
      </section>
      <section className="login-page__access" aria-label="Acesso ao sistema">
        <LoginForm />
      </section>
    </main>
  )
}
