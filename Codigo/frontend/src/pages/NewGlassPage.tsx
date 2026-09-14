import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowLeft, Layers } from 'lucide-react'
import { useAuth } from '../features/auth/hooks/use-auth'
import { createGlass } from '../features/glasses/api/glasses-api'
import { AppShell } from '../shared/components/AppShell'
import './WorkflowPages.css'

const QUICK_THICKNESSES = [3, 4, 5, 6, 8, 10, 12, 15, 19]
const FINISH_OPTIONS = ['Lapidado', 'Bisotê', 'Canto Moeda', 'Jateado', 'Sem acabamento']
const COLOR_OPTIONS = ['Incolor', 'Fumê', 'Verde', 'Bronze', 'Extra Claro']

export function NewGlassPage() {
  const navigate = useNavigate()
  const { user } = useAuth()

  const [name, setName] = useState('')
  const [thicknessMm, setThicknessMm] = useState<number>(8)
  const [finish, setFinish] = useState('Lapidado')
  const [color, setColor] = useState('Incolor')
  const [pricePerSquareMeter, setPricePerSquareMeter] = useState<string>('150.00')

  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    setSuccess(null)

    const price = Number.parseFloat(pricePerSquareMeter.replace(',', '.'))
    if (Number.isNaN(price) || price <= 0) {
      setError('Informe um preço por metro quadrado válido e maior que zero.')
      setBusy(false)
      return
    }

    if (!thicknessMm || thicknessMm <= 0) {
      setError('A espessura deve ser maior que zero.')
      setBusy(false)
      return
    }

    try {
      await createGlass({
        name: name.trim(),
        thicknessMm: Number(thicknessMm),
        finish: finish.trim(),
        color: color.trim(),
        pricePerSquareMeter: price,
      })

      setSuccess('Vidro cadastrado com sucesso no catálogo!')
      setTimeout(() => {
        void navigate('/glasses')
      }, 1000)
    } catch (err: unknown) {
      const isDuplicate =
        typeof err === 'object' &&
        err !== null &&
        'code' in err &&
        (err as { code: string }).code === 'DUPLICATE_GLASS'

      if (isDuplicate) {
        setError('Já existe um vidro cadastrado com este mesmo nome, espessura, acabamento e cor.')
      } else {
        setError('Não foi possível cadastrar o vidro. Verifique as informações preenchidas.')
      }
    } finally {
      setBusy(false)
    }
  }

  if (!user?.permissions.includes('GLASS_CREATE')) {
    return (
      <AppShell>
        <section className="workflow-card">
          <h1>Acesso não permitido</h1>
          <p>Você não possui permissão para cadastrar novos vidros.</p>
        </section>
      </AppShell>
    )
  }

  return (
    <AppShell>
      <section className="workflow-card workflow-card--narrow">
        <p className="eyebrow">Catálogo de Vidros</p>
        <h1>Novo Vidro</h1>
        <p>Cadastre um novo tipo de vidro com suas especificações técnicas e preço por m².</p>

        {error ? (
          <p className="workflow-feedback workflow-error" role="alert">
            {error}
          </p>
        ) : null}

        {success ? (
          <p className="workflow-feedback" role="status">
            {success}
          </p>
        ) : null}

        <form className="workflow-form" onSubmit={(event) => void submit(event)}>
          <label>
            Nome descritivo do vidro
            <input
              required
              maxLength={120}
              placeholder="Ex: Vidro Temperado, Vidro Laminado, etc."
              value={name}
              onChange={(event) => setName(event.target.value)}
              disabled={busy}
            />
          </label>

          <fieldset style={{ border: 'none', padding: 0, margin: '0.5rem 0 0' }}>
            <legend style={{ fontWeight: 700, marginBottom: '0.4rem', color: 'var(--color-blue-950)' }}>
              Espessura (mm)
            </legend>
            <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap', marginBottom: '0.6rem' }}>
              {QUICK_THICKNESSES.map((t) => (
                <button
                  key={t}
                  type="button"
                  className={`button ${thicknessMm === t ? 'button--primary' : 'button--secondary'}`}
                  style={{
                    padding: '0.35rem 0.75rem',
                    fontSize: '0.82rem',
                    minHeight: '2rem',
                    borderRadius: 'var(--radius-xs)',
                  }}
                  onClick={() => setThicknessMm(t)}
                  disabled={busy}
                >
                  {t} mm
                </button>
              ))}
            </div>
            <input
              type="number"
              step="0.01"
              min="0.5"
              max="100"
              required
              value={thicknessMm}
              onChange={(event) => setThicknessMm(Number.parseFloat(event.target.value) || 0)}
              disabled={busy}
              placeholder="Ou digite a espessura personalizada"
            />
          </fieldset>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
            <label>
              Acabamento
              <select
                className="filter-select"
                value={finish}
                onChange={(event) => setFinish(event.target.value)}
                disabled={busy}
              >
                {FINISH_OPTIONS.map((f) => (
                  <option key={f} value={f}>
                    {f}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Cor
              <select
                className="filter-select"
                value={color}
                onChange={(event) => setColor(event.target.value)}
                disabled={busy}
              >
                {COLOR_OPTIONS.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <label>
            Preço por metro quadrado (R$/m²)
            <input
              type="number"
              step="0.01"
              min="0.01"
              required
              value={pricePerSquareMeter}
              onChange={(event) => setPricePerSquareMeter(event.target.value)}
              disabled={busy}
            />
          </label>

          <div className="workflow-actions" style={{ marginTop: '1rem' }}>
            <button className="button button--primary" type="submit" disabled={busy || !name.trim()}>
              <Layers size={16} style={{ marginRight: '0.45rem' }} />
              {busy ? 'Cadastrando...' : 'Cadastrar vidro'}
            </button>
            <Link className="button button--secondary" to="/glasses">
              <ArrowLeft size={16} style={{ marginRight: '0.35rem' }} />
              Cancelar
            </Link>
          </div>
        </form>
      </section>
    </AppShell>
  )
}
