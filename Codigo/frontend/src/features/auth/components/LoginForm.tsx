import { useEffect, useState, type SyntheticEvent } from 'react'
import { Link } from 'react-router-dom'
import { Eye, EyeOff } from 'lucide-react'

import { useAuth } from '../hooks/use-auth'
import { getAuthenticationConfiguration, startGoogle } from '../api/auth-api'
import './LoginForm.css'

export function LoginForm() {
  const { clearError, error, isSubmitting, login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [googleEnabled, setGoogleEnabled] = useState(false)

  useEffect(() => {
    void getAuthenticationConfiguration().then((config) => setGoogleEnabled(config.googleEnabled)).catch(() => undefined)
  }, [])

  async function handleSubmit(event: SyntheticEvent<HTMLFormElement>) {
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
          <div className="password-field">
            <input
              id="password"
              name="password"
              type={showPassword ? 'text' : 'password'}
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
            <button
              className="password-field__toggle"
              type="button"
              onClick={() => setShowPassword((current) => !current)}
              disabled={isSubmitting}
              aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
              aria-pressed={showPassword}
            >
              {showPassword ? <EyeOff aria-hidden="true" size={18} /> : <Eye aria-hidden="true" size={18} />}
            </button>
          </div>
        </div>

        <div className="login-form__feedback" aria-live="polite">
          {error ? <p role="alert">{error}</p> : null}
        </div>

        <button className="button button--primary" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Entrando...' : 'Entrar'}
        </button>
        <Link className="login-form__forgot" to="/forgot-password">Esqueci minha senha</Link>
      </form>

      {googleEnabled ? (
        <div className="login-card__google">
          <span>ou</span>
          <button className="button button--secondary" type="button" onClick={() => void startGoogle()}>
            Entrar com Google
          </button>
        </div>
      ) : null}

      <p className="login-card__support">Problemas para acessar? Procure o administrador do Glazr.</p>
    </div>
  )
}
