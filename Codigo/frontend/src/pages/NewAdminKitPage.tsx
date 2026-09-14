import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../features/auth/hooks/use-auth'
import { createKit, type KitSummary } from '../features/kits/api/kits-api'
import { AppShell } from '../shared/components/AppShell'
import './WorkflowPages.css'

interface ComponentDraft { name: string; quantity: string; notes: string }

const emptyComponent = (): ComponentDraft => ({ name: '', quantity: '1', notes: '' })

export function NewAdminKitPage() {
  const { user } = useAuth(); const [name, setName] = useState(''); const [color, setColor] = useState(''); const [price, setPrice] = useState(''); const [components, setComponents] = useState<ComponentDraft[]>([emptyComponent()]); const [created, setCreated] = useState<KitSummary | null>(null); const [error, setError] = useState<string | null>(null); const [busy, setBusy] = useState(false)
  function updateComponent(index: number, patch: Partial<ComponentDraft>) { setComponents((current) => current.map((component, position) => position === index ? { ...component, ...patch } : component)) }
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(null)
    try {
      setCreated(await createKit({
        name: name.trim(), color: color.trim(), price: Number(price),
        components: components.map((component) => ({
          name: component.name.trim(), quantity: Number(component.quantity), notes: component.notes.trim() || null,
        })),
      }))
    } catch { setError('Não foi possível cadastrar o kit. Confira os dados e se o nome já está em uso.') } finally { setBusy(false) }
  }
  if (!user?.permissions.includes('KIT_CREATE')) return <AppShell><section className="workflow-card"><h1>Acesso não permitido</h1></section></AppShell>
  return <AppShell><section className="workflow-card workflow-card--narrow"><p className="eyebrow">Novo cadastro</p><h1>Cadastrar kit</h1><p>O kit agrupa componentes (vidros, perfis, ferragens) com um preço de referência para orçamentos.</p>
    {created ? <div className="workflow-feedback"><strong>{created.name} foi cadastrado.</strong><br /><Link to="/admin/kits">Ver kits</Link></div> : <form className="workflow-form" onSubmit={(event) => void submit(event)}>{error ? <p className="workflow-feedback workflow-error" role="alert">{error}</p> : null}<label>Nome<input required maxLength={80} value={name} onChange={(event) => setName(event.target.value)} /></label><label>Cor<input required maxLength={32} value={color} onChange={(event) => setColor(event.target.value)} /></label><label>Preço (R$)<input type="number" required min={0} step="0.01" value={price} onChange={(event) => setPrice(event.target.value)} /></label><fieldset><legend>Componentes</legend>{components.map((component, index) => <div className="profile-options" key={index}><label>Componente<input required maxLength={120} value={component.name} onChange={(event) => updateComponent(index, { name: event.target.value })} /></label><label>Quantidade<input type="number" required min={1} value={component.quantity} onChange={(event) => updateComponent(index, { quantity: event.target.value })} /></label><label>Observação<input maxLength={300} value={component.notes} onChange={(event) => updateComponent(index, { notes: event.target.value })} /></label><button className="button button--secondary" type="button" disabled={components.length === 1} onClick={() => setComponents((current) => current.filter((_, position) => position !== index))}>Remover</button></div>)}<button className="button button--secondary" type="button" onClick={() => setComponents((current) => [...current, emptyComponent()])}>Adicionar componente</button></fieldset><button className="button button--primary" disabled={busy}>{busy ? 'Cadastrando...' : 'Cadastrar kit'}</button></form>}
  </section></AppShell>
}