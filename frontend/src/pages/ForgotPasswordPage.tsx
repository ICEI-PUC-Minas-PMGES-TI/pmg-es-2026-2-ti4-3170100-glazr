import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { requestPasswordReset } from '../features/password/api/password-api'
import './WorkflowPages.css'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState(''); const [sent, setSent] = useState(false); const [busy, setBusy] = useState(false)
  async function submit(event: FormEvent) { event.preventDefault(); setBusy(true); try { await requestPasswordReset(email.trim()); setSent(true) } finally { setBusy(false) } }
  return <main className="workflow-page"><section className="workflow-card workflow-card--narrow">
    <p className="eyebrow">Recuperação de acesso</p><h1>Redefinir senha</h1>
    {sent ? <p className="workflow-feedback">Se existir uma conta elegível, enviaremos as instruções para o e-mail informado.</p> : <form className="workflow-form" onSubmit={(event) => void submit(event)}><p>Informe seu e-mail corporativo. A resposta é sempre a mesma para proteger as contas cadastradas.</p><label>E-mail<input type="email" required maxLength={320} value={email} onChange={(event) => setEmail(event.target.value)} /></label><button className="button button--primary" disabled={busy}>{busy ? 'Enviando...' : 'Enviar instruções'}</button></form>}
    <p><Link to="/login">Voltar ao login</Link></p>
  </section></main>
}
