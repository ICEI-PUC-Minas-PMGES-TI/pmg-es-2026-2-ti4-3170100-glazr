import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { acceptInvitation, inspectInvitation, startInvitationGoogle, type InvitationInspection } from '../features/invitation/api/invitation-api'
import { ApiError } from '../shared/api/api-error'
import { useFragmentToken } from '../shared/hooks/use-fragment-token'
import './WorkflowPages.css'

const invitationError = (error: unknown) => error instanceof ApiError
  ? ({ INVITATION_EXPIRED: 'Este convite expirou.', INVITATION_REVOKED: 'Este convite foi revogado.', INVITATION_ALREADY_USED: 'Este convite já foi utilizado.' }[error.code ?? ''] ?? 'Não foi possível validar este convite.')
  : 'Não foi possível validar este convite.'

export function ActivatePage() {
  const token = useFragmentToken()
  const [inspection, setInspection] = useState<InvitationInspection | null>(null)
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [error, setError] = useState<string | null>(token ? null : 'O link de convite é inválido.')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!token) return
    void inspectInvitation(token).then(setInspection).catch((reason: unknown) => setError(invitationError(reason)))
  }, [token])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!token || password !== confirmation) { setError('As senhas precisam ser iguais.'); return }
    setSubmitting(true); setError(null)
    try { await acceptInvitation(token, password); window.location.assign('/') }
    catch (reason) { setError(invitationError(reason)); setSubmitting(false) }
  }

  return <main className="workflow-page"><section className="workflow-card workflow-card--narrow">
    <p className="eyebrow">Ativação de acesso</p><h1>Configure sua conta Glazr</h1>
    {error ? <p className="workflow-feedback workflow-error" role="alert">{error}</p> : null}
    {!inspection && !error ? <p>Validando convite...</p> : null}
    {inspection ? <>
      <p>Olá, {inspection.displayName}. Este convite foi enviado para <strong>{inspection.email}</strong>.</p>
      <form className="workflow-form" onSubmit={(event) => void submit(event)}>
        <label>Crie uma senha<input type="password" minLength={12} maxLength={128} required value={password} onChange={(event) => setPassword(event.target.value)} /></label>
        <label>Confirme a senha<input type="password" minLength={12} maxLength={128} required value={confirmation} onChange={(event) => setConfirmation(event.target.value)} /></label>
        <button className="button button--primary" disabled={submitting}>{submitting ? 'Ativando...' : 'Criar senha e entrar'}</button>
      </form>
      {inspection.availableMethods.includes('GOOGLE') ? <div className="login-card__google"><span>ou</span><button className="button button--secondary" type="button" disabled={submitting} onClick={() => token && void startInvitationGoogle(token)}>Continuar com Google</button><small>Use a conta Google com o mesmo e-mail do convite.</small></div> : null}
    </> : null}
    {error ? <p>Solicite um novo convite ao administrador. <Link to="/login">Voltar ao login</Link></p> : null}
  </section></main>
}
