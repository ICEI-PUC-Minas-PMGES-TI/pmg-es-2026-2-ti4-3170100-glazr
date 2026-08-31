import { useEffect, useState, type FormEvent } from 'react'
import { getAuthenticationConfiguration, startGoogle } from '../features/auth/api/auth-api'
import { useAuth } from '../features/auth/hooks/use-auth'
import { changePassword, requestPasswordReset } from '../features/password/api/password-api'
import { AppShell } from '../shared/components/AppShell'
import './WorkflowPages.css'

export function AccountSecurityPage() {
  const { user } = useAuth(); const [googleEnabled, setGoogleEnabled] = useState(false); const [current, setCurrent] = useState(''); const [next, setNext] = useState(''); const [confirmation, setConfirmation] = useState(''); const [feedback, setFeedback] = useState<string | null>(null); const [busy, setBusy] = useState(false)
  useEffect(() => { void getAuthenticationConfiguration().then((value) => setGoogleEnabled(value.googleEnabled)).catch(() => undefined) }, [])
  if (!user) return null
  const userEmail = user.email
  const hasPassword = user.authenticationMethods.includes('PASSWORD'); const hasGoogle = user.authenticationMethods.includes('GOOGLE')
  async function submit(event: FormEvent) { event.preventDefault(); if (next !== confirmation) { setFeedback('As senhas precisam ser iguais.'); return } setBusy(true); try { await changePassword(current, next); setFeedback('Senha alterada. As outras sessões foram encerradas.'); setCurrent(''); setNext(''); setConfirmation('') } catch { setFeedback('Não foi possível alterar a senha. Confira a senha atual.') } finally { setBusy(false) } }
  async function createPassword() { setBusy(true); try { await requestPasswordReset(userEmail); setFeedback('Se a conta for elegível, enviaremos um link para criar a senha.') } finally { setBusy(false) } }
  return <AppShell><section className="workflow-card"><p className="eyebrow">Sua conta</p><h1>Segurança</h1><p>Métodos vinculados: {user.authenticationMethods.map((method) => <span className="badge" key={method}>{method}</span>)}</p>{feedback ? <p className="workflow-feedback" aria-live="polite">{feedback}</p> : null}
    {hasPassword ? <form className="workflow-form" onSubmit={(event) => void submit(event)}><h2>Alterar senha</h2><label>Senha atual<input type="password" required maxLength={128} value={current} onChange={(event) => setCurrent(event.target.value)} /></label><label>Nova senha<input type="password" required minLength={12} maxLength={128} value={next} onChange={(event) => setNext(event.target.value)} /></label><label>Confirme a nova senha<input type="password" required minLength={12} maxLength={128} value={confirmation} onChange={(event) => setConfirmation(event.target.value)} /></label><button className="button button--primary" disabled={busy}>Alterar senha</button></form> : <div><h2>Senha local</h2><p>Você ainda não possui uma senha local.</p><button className="button button--secondary" disabled={busy} onClick={() => void createPassword()}>Enviar link para criar senha</button></div>}
    {googleEnabled && !hasGoogle ? <div><h2>Conta Google</h2><p>Vincule somente a conta Google com o e-mail {user.email}.</p><button className="button button--secondary" onClick={() => void startGoogle('/api/v1/auth/google/link/start')}>Vincular Google</button></div> : null}
  </section></AppShell>
}
