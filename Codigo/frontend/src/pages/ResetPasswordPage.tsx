import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { completePasswordReset } from '../features/password/api/password-api'
import { ApiError } from '../shared/api/api-error'
import { useFragmentToken } from '../shared/hooks/use-fragment-token'
import './WorkflowPages.css'

export function ResetPasswordPage() {
  const token = useFragmentToken(); const [password, setPassword] = useState(''); const [confirmation, setConfirmation] = useState(''); const [done, setDone] = useState(false); const [error, setError] = useState<string | null>(token ? null : 'O link de redefinição é inválido.'); const [busy, setBusy] = useState(false)
  async function submit(event: FormEvent) { event.preventDefault(); if (!token || password !== confirmation) { setError('As senhas precisam ser iguais.'); return } setBusy(true); try { await completePasswordReset(token, password); setDone(true) } catch (reason) { setError(reason instanceof ApiError && reason.code === 'PASSWORD_RESET_EXPIRED' ? 'Este link expirou.' : 'Não foi possível redefinir a senha.') } finally { setBusy(false) } }
  return <main className="workflow-page"><section className="workflow-card workflow-card--narrow"><p className="eyebrow">Nova credencial</p><h1>Crie uma nova senha</h1>
    {done ? <p className="workflow-feedback">Senha atualizada. Todas as sessões anteriores foram encerradas. <Link to="/login">Entrar</Link></p> : <form className="workflow-form" onSubmit={(event) => void submit(event)}>{error ? <p className="workflow-feedback workflow-error" role="alert">{error}</p> : null}<label>Nova senha<input type="password" minLength={12} maxLength={128} required value={password} onChange={(event) => setPassword(event.target.value)} /></label><label>Confirme a senha<input type="password" minLength={12} maxLength={128} required value={confirmation} onChange={(event) => setConfirmation(event.target.value)} /></label><button className="button button--primary" disabled={busy || !token}>{busy ? 'Salvando...' : 'Redefinir senha'}</button></form>}
  </section></main>
}
