import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Layers } from 'lucide-react'
import { useAuth } from '../features/auth/hooks/use-auth'
import {
  getGlass,
  updateGlass,
  updateGlassStatus,
  type GlassSummary,
} from '../features/glasses/api/glasses-api'
import { AppShell } from '../shared/components/AppShell'
import './WorkflowPages.css'

const QUICK_THICKNESSES = [3, 4, 5, 6, 8, 10, 12, 15, 19]
const FINISH_OPTIONS = ['Lapidado', 'Bisotê', 'Canto Moeda', 'Jateado', 'Sem acabamento']
const COLOR_OPTIONS = ['Incolor', 'Fumê', 'Verde', 'Bronze', 'Extra Claro']

export function EditGlassPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [targetGlass, setTargetGlass] = useState<GlassSummary | null>(null)
  const [name, setName] = useState('')
  const [thicknessMm, setThicknessMm] = useState<number>(8)
  const [finish, setFinish] = useState('Lapidado')
  const [color, setColor] = useState('Incolor')
  const [pricePerSquareMeter, setPricePerSquareMeter] = useState<string>('150.00')
  const [active, setActive] = useState(true)
  const [originalActive, setOriginalActive] = useState(true)

  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    getGlass(id)
      .then((glass) => {
        setTargetGlass(glass)
        setName(glass.name)
        setThicknessMm(glass.thicknessMm)
        setFinish(glass.finish)
        setColor(glass.color)
        setPricePerSquareMeter(String(glass.pricePerSquareMeter))
        setActive(glass.active)
        setOriginalActive(glass.active)
      })
      .catch(() => setError('Não foi possível carregar os dados do vidro.'))
      .finally(() => setLoading(false))
  }, [id])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!id) return

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
      await updateGlass(id, {
        name: name.trim(),
        thicknessMm: Number(thicknessMm),
        finish: finish.trim(),
        color: color.trim(),
        pricePerSquareMeter: price,
      })

      if (active !== originalActive) {
        await updateGlassStatus(id, active)
        setOriginalActive(active)
      }

      setSuccess('Vidro atualizado com sucesso no catálogo!')
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
        setError('Já existe um outro vidro cadastrado com este mesmo nome, espessura, acabamento e cor.')
      } else {
        setError('Não foi possível atualizar o vidro. Verifique as informações preenchidas.')
      }
    } finally {
      setBusy(false)
    }
  }

  if (!user?.permissions.includes('GLASS_UPDATE')) {
    return (
      <AppShell>
        <section className="workflow-card">
          <h1>Acesso não permitido</h1>
          <p>Você não possui permissão para editar vidros.</p>
        </section>
      </AppShell>
    )
  }

  if (loading) {
    return (
      <AppShell>
        <section className="workflow-card workflow-card--narrow">
          <p>Carregando dados do vidro...</p>
        </section>
      </AppShell>
    )
  }

  if (!targetGlass) {
    return (
      <AppShell>
        <section className="workflow-card workflow-card--narrow">
          <h1>Vidro não encontrado</h1>
          <p>O vidro solicitado não existe ou foi removido do catálogo.</p>
          <Link className="button button--secondary" to="/glasses">
            Voltar para o catálogo
          </Link>
        </section>
      </AppShell>
    )
  }

  return (
    <AppShell>
      <section className="workflow-card workflow-card--narrow">
        <p className="eyebrow">Edição de Produto</p>
        <h1>Editar Vidro</h1>
        <p>Atualize as especificações técnicas, status no catálogo e preço por m².</p>

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

          <label>
            Status no Catálogo
            <select
              className="filter-select"
              value={active ? 'true' : 'false'}
              onChange={(event) => setActive(event.target.value === 'true')}
              disabled={busy}
            >
              <option value="true">Ativo (Disponível para orçamentos e pedidos)</option>
              <option value="false">Inativo (Oculto em novos orçamentos)</option>
            </select>
          </label>

          <div className="workflow-actions" style={{ marginTop: '1rem' }}>
            <button className="button button--primary" type="submit" disabled={busy || !name.trim()}>
              <Layers size={16} style={{ marginRight: '0.45rem' }} />
              {busy ? 'Salvando...' : 'Salvar alterações'}
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
