import { useState, type FormEvent } from 'react'

import { useAuth } from '../hooks/use-auth'
import './LoginForm.css'

export function LoginForm() {
  const { clearError, error, isSubmitting, login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await login({ email: email.trim(), password })
    setPassword('')
  }

  return (
    <div className="login-card">
      <header className="login-card__header">
        <p className="eyebrow">Bem-vindo de volta</p>
        <h1>Acesse sua conta</h1>
        <p>Entre com as credenciais fornecidas pelo administrador.</p>
      </header>

      <form className="login-form" onSubmit={(event) => void handleSubmit(event)}>
        <div className="form-field">
          <label htmlFor="email">E-mail</label>
          <input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            required
            maxLength={320}
            value={email}
            onChange={(event) => {
              setEmail(event.target.value)
              clearError()
            }}
            placeholder="voce@empresa.com.br"
            disabled={isSubmitting}
          />
        </div>

        <div className="form-field">
          <label htmlFor="password">Senha</label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
            maxLength={128}
            value={password}
            onChange={(event) => {
              setPassword(event.target.value)
              clearError()
            }}
            placeholder="Digite sua senha"
            disabled={isSubmitting}
          />
        </div>

        <div className="login-form__feedback" aria-live="polite">
          {error ? <p role="alert">{error}</p> : null}
        </div>

        <button className="button button--primary" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Entrando...' : 'Entrar'}
        </button>
      </form>

      <p className="login-card__support">Problemas para acessar? Procure o administrador do Glazr.</p>
    </div>
  )
}
