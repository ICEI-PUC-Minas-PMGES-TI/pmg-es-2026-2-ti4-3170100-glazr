import { Link } from 'react-router-dom'

import { LoginForm } from '../features/auth/components/LoginForm'
import './LoginPage.css'

export function LoginPage() {
  return (
    <main className="login-page">
      <section className="login-page__intro" aria-labelledby="glazr-brand">
        <video
          className="login-page__video"
          autoPlay
          loop
          muted
          playsInline
          src="/Glass_character_waving_hello_1080p_20260914105221.mp4"
        />
        <div className="login-page__overlay" aria-hidden="true" />
        <Link className="brand" to="/" id="glazr-brand" aria-label="Glazr — página inicial">
          <img src="/glazr-fav-icon.png" alt="Glazr Logo" className="brand__logo" />
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
