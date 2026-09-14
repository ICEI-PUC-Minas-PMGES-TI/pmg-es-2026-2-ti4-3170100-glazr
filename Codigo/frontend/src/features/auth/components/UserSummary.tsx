import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { AlertTriangle, ArrowRight, Layers, Package, Shield, Users } from 'lucide-react'
import { useAuth } from '../hooks/use-auth'
import { listLowStockAlerts, type GlassStockSummary } from '../../inventory/api/inventory-api'
import './UserSummary.css'

export function UserSummary() {
  const { user } = useAuth()
  const [lowStock, setLowStock] = useState<ReadonlyArray<GlassStockSummary>>([])
  const [alertsUnavailable, setAlertsUnavailable] = useState(false)
  const canReadInventory = user?.permissions.includes('GLASS_READ') ?? false

  useEffect(() => {
    if (!canReadInventory) return
    void listLowStockAlerts()
      .then((items) => setLowStock(items))
      .catch(() => setAlertsUnavailable(true))
  }, [canReadInventory])

  if (!user) return null

  const canManageUsers = user.permissions.includes('USER_READ') || user.permissions.includes('USER_CREATE')
  const canManageGlasses = user.permissions.includes('GLASS_READ') || user.permissions.includes('GLASS_CREATE')
  const canManageKits = user.permissions.includes('KIT_READ') || user.permissions.includes('KIT_CREATE')

  return (
    <section className="user-summary" aria-labelledby="welcome-title">
      <div className="user-summary__hero">
        <div>
          <p className="eyebrow">Painel Principal</p>
          <h1 id="welcome-title">Olá, {user.displayName}</h1>
        </div>
      </div>

      <div className="user-summary__section">
        {canReadInventory ? <section className="inventory-dashboard" aria-labelledby="inventory-alerts-title">
          <div>
            <p className="eyebrow">Estoque</p>
            <h2 id="inventory-alerts-title">Alertas de estoque</h2>
          </div>
          {alertsUnavailable ? <p>Não foi possível consultar os alertas de estoque agora.</p> : null}
          {!alertsUnavailable && lowStock.length === 0 ? <p>Nenhum tipo de vidro atingiu o estoque mínimo.</p> : null}
          {!alertsUnavailable && lowStock.length > 0 ? <div className="inventory-dashboard__alerts">
            <AlertTriangle size={20} aria-hidden="true" />
            <p><strong>{lowStock.length} tipo(s) de vidro</strong> estão no estoque mínimo ou abaixo dele.</p>
            <Link className="button button--secondary" to="/inventory">Ver estoque</Link>
          </div> : null}
        </section> : null}

        <h2 className="section-title">Módulos do Sistema</h2>
        <div className="shortcut-grid">
          {canManageUsers ? (
            <Link to="/admin/users" className="shortcut-card">
              <div className="shortcut-card__header">
                <div className="shortcut-card__icon">
                  <Users size={20} strokeWidth={2} />
                </div>
                <span className="shortcut-card__badge">Administração</span>
              </div>
              <h3 className="shortcut-card__title">Funcionários</h3>
              <p className="shortcut-card__desc">
                Gerencie funcionários, envie convites de acesso e configure perfis de permissão.
              </p>
              <div className="shortcut-card__action">
                <span>Acessar módulo</span>
                <ArrowRight size={16} />
              </div>
            </Link>
          ) : null}

          {canManageGlasses ? (
            <Link to="/glasses" className="shortcut-card">
              <div className="shortcut-card__header">
                <div className="shortcut-card__icon">
                  <Layers size={20} strokeWidth={2} />
                </div>
                <span className="shortcut-card__badge">Catálogo</span>
              </div>
              <h3 className="shortcut-card__title">Vidros e Chapas</h3>
              <p className="shortcut-card__desc">
                Consulte e cadastre vidros por espessura, acabamento, cor e precificação por metro quadrado.
              </p>
              <div className="shortcut-card__action">
                <span>Acessar catálogo</span>
                <ArrowRight size={16} />
              </div>
            </Link>
          ) : null}

          {canReadInventory ? (
            <Link to="/inventory" className="shortcut-card">
              <div className="shortcut-card__header">
                <div className="shortcut-card__icon">
                  <Layers size={20} strokeWidth={2} />
                </div>
                <span className="shortcut-card__badge">Materiais</span>
              </div>
              <h3 className="shortcut-card__title">Estoque de Chapas</h3>
              <p className="shortcut-card__desc">
                Acompanhe saldo em m², configure mínimos e registre recebimentos, cortes e sobras úteis.
              </p>
              <div className="shortcut-card__action">
                <span>Acessar estoque</span>
                <ArrowRight size={16} />
              </div>
            </Link>
          ) : null}

          {canManageKits ? (
            <Link to="/admin/kits" className="shortcut-card">
              <div className="shortcut-card__header">
                <div className="shortcut-card__icon">
                  <Package size={20} strokeWidth={2} />
                </div>
                <span className="shortcut-card__badge">Catálogo</span>
              </div>
              <h3 className="shortcut-card__title">Kits e Produtos</h3>
              <p className="shortcut-card__desc">
                Cadastre e consulte kits de vidraçaria, vidros, cores e especificações de componentes.
              </p>
              <div className="shortcut-card__action">
                <span>Acessar módulo</span>
                <ArrowRight size={16} />
              </div>
            </Link>
          ) : null}

          <Link to="/account/security" className="shortcut-card">
            <div className="shortcut-card__header">
              <div className="shortcut-card__icon">
                <Shield size={20} strokeWidth={2} />
              </div>
              <span className="shortcut-card__badge">Segurança</span>
            </div>
            <h3 className="shortcut-card__title">Segurança da Conta</h3>
            <p className="shortcut-card__desc">
              Altere sua senha local e gerencie a autenticação vinculada à sua conta Google.
            </p>
            <div className="shortcut-card__action">
              <span>Configurar segurança</span>
              <ArrowRight size={16} />
            </div>
          </Link>
        </div>
      </div>
    </section>
  )
}
