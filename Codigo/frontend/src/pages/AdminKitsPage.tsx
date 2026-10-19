import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../features/auth/hooks/use-auth'
import { deleteKit, listKits, type KitSummary } from '../features/kits/api/kits-api'
import { AppShell } from '../shared/components/AppShell'
import { currencyFormatter } from '../shared/format/formatters'
import './WorkflowPages.css'

export function AdminKitsPage() {
  const { user } = useAuth()
  const [kits, setKits] = useState<KitSummary[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState<string | null>(null)

  const load = useCallback(() => {
    return listKits()
      .then((page) => setKits(page.content))
      .catch(() => setError('Não foi possível carregar os kits.'))
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  async function act(id: string, operation: () => Promise<unknown>) {
    setBusy(id)
    setError(null)
    try {
      await operation()
      await load()
    } catch {
      setError('Não foi possível concluir a ação.')
    } finally {
      setBusy(null)
    }
  }

  if (!user?.permissions.includes('KIT_READ')) {
    return (
      <AppShell>
        <section className="workflow-card">
          <h1>Acesso não permitido</h1>
        </section>
      </AppShell>
    )
  }

  return (
    <AppShell>
      <section className="workflow-card">
        <div className="workflow-actions">
          <div>
            <p className="eyebrow">Administração</p>
            <h1>Kits</h1>
          </div>
          {user.permissions.includes('KIT_CREATE') ? (
            <Link className="button button--primary" to="/admin/kits/new">
              Novo kit
            </Link>
          ) : null}
        </div>

        {error ? (
          <p className="workflow-feedback workflow-error" role="alert">
            {error}
          </p>
        ) : null}

        <table className="data-table">
          <thead>
            <tr>
              <th>Kit</th>
              <th>Preço</th>
              <th>Componentes</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {kits.length === 0 ? (
              <tr>
                <td colSpan={4} style={{ textAlign: 'center', padding: '2rem' }}>
                  Nenhum kit cadastrado.
                </td>
              </tr>
            ) : (
              kits.map((kit) => (
                <tr key={kit.id}>
                  <td>
                    <strong>{kit.name}</strong>
                    <br />
                    <span className="badge">{kit.color}</span>
                  </td>
                  <td>{currencyFormatter.format(kit.price)}</td>
                  <td>
                    {kit.components.map((component, index) => (
                      <span className="badge" key={index}>
                        {component.quantity}× {component.name}
                      </span>
                    ))}
                  </td>
                  <td>
                    <div className="workflow-actions">
                      {user.permissions.includes('KIT_DELETE') ? (
                        <button
                          className="button button--secondary"
                          disabled={busy === kit.id}
                          onClick={() => void act(kit.id, () => deleteKit(kit.id))}
                        >
                          Excluir
                        </button>
                      ) : null}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </section>
    </AppShell>
  )
}