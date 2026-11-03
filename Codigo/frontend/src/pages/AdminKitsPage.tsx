import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/hooks/use-auth";
import {
  deleteKit,
  listKits,
  type KitSummary,
} from "../features/kits/api/kits-api";
import {
  ActionsMenu,
  type ActionsMenuItem,
} from "../shared/components/ActionsMenu";
import { AppShell } from "../shared/components/AppShell";
import { currencyFormatter } from "../shared/format/formatters";
import "./WorkflowPages.css";
import { Pencil, Trash2 } from "lucide-react";

const VISIBLE_COMPONENTS = 4;

function KitComponentsList({ components }: { components: KitSummary["components"] }) {
  const [expanded, setExpanded] = useState(false);
  const hiddenCount = components.length - VISIBLE_COMPONENTS;
  const visible = expanded ? components : components.slice(0, VISIBLE_COMPONENTS);

  return (
    <div className="kit-components">
      {visible.map((component, index) => (
        <span className="badge kit-components__item" key={index} title={component.name}>
          <span className="kit-components__name">
            {component.quantity}× {component.name}
          </span>
        </span>
      ))}
      {hiddenCount > 0 ? (
        <button
          type="button"
          className="kit-components__toggle"
          aria-expanded={expanded}
          onClick={() => setExpanded((current) => !current)}
        >
          {expanded ? "Mostrar menos" : `+${hiddenCount} ${hiddenCount === 1 ? "componente" : "componentes"}`}
        </button>
      ) : null}
    </div>
  );
}

export function AdminKitsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [kits, setKits] = useState<KitSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  const load = useCallback(() => {
    return listKits()
      .then((page) => setKits(page.content))
      .catch(() => setError("Não foi possível carregar os kits."));
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function act(id: string, operation: () => Promise<unknown>) {
    setBusy(id);
    setError(null);
    try {
      await operation();
      await load();
    } catch {
      setError("Não foi possível concluir a ação.");
    } finally {
      setBusy(null);
    }
  }

  function actionsFor(kit: KitSummary): ActionsMenuItem[] {
    const permissions = user?.permissions ?? [];
    const actions: ActionsMenuItem[] = [];
    if (permissions.includes("KIT_UPDATE")) {
      actions.push({
        label: "Editar",
        icon: <Pencil size={16} aria-hidden="true" />,
        onSelect: () => void navigate(`/admin/kits/${kit.id}/edit`),
      });
    }
    if (permissions.includes("KIT_DELETE")) {
      actions.push({
        label: "Excluir",
        icon: <Trash2 size={16} aria-hidden="true" />,
        tone: "danger",
        onSelect: () => {
          if (window.confirm(`Deseja realmente excluir o kit "${kit.name}"?`)) {
            void act(kit.id, () => deleteKit(kit.id));
          }
        },
      });
    }
    return actions;
  }

  if (!user?.permissions.includes("KIT_READ")) {
    return (
      <AppShell>
        <section className="workflow-card">
          <h1>Acesso não permitido</h1>
        </section>
      </AppShell>
    );
  }

  return (
    <AppShell>
      <section className="workflow-card">
        <div className="workflow-actions">
          <div>
            <p className="eyebrow">Administração</p>
            <h1>Kits</h1>
          </div>
          {user.permissions.includes("KIT_CREATE") ? (
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

        <table className="data-table kits-table">
          <colgroup>
            <col className="kits-table__kit" />
            <col className="kits-table__price" />
            <col />
            <col className="kits-table__actions" />
          </colgroup>
          <thead>
            <tr>
              <th>Kit</th>
              <th className="kits-table__number">Preço</th>
              <th>Componentes</th>
              <th className="kits-table__center">Ações</th>
            </tr>
          </thead>
          <tbody>
            {kits.length === 0 ? (
              <tr>
                <td
                  colSpan={4}
                  style={{ textAlign: "center", padding: "2rem" }}
                >
                  Nenhum kit cadastrado.
                </td>
              </tr>
            ) : (
              kits.map((kit) => (
                <tr key={kit.id}>
                  <td>
                    <div className="kits-table__identity">
                      <strong>{kit.name}</strong>
                      <span className="badge">{kit.color}</span>
                    </div>
                  </td>
                  <td className="kits-table__number">
                    <strong>{currencyFormatter.format(kit.price)}</strong>
                  </td>
                  <td>
                    <KitComponentsList components={kit.components} />
                  </td>
                  <td className="kits-table__center">
                    <ActionsMenu
                      label={`Ações para ${kit.name}`}
                      disabled={busy === kit.id}
                      items={actionsFor(kit)}
                    />
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </section>
    </AppShell>
  );
}
