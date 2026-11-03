import { useEffect, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../features/auth/hooks/use-auth";
import {
  createKit,
  getKit,
  updateKit,
  type KitSummary,
} from "../features/kits/api/kits-api";
import { AppShell } from "../shared/components/AppShell";
import "./WorkflowPages.css";

interface ComponentDraft {
  name: string;
  quantity: string;
  notes: string;
}

const emptyComponent = (): ComponentDraft => ({
  name: "",
  quantity: "1",
  notes: "",
});

export function NewAdminKitPage() {
  return <KitFormPage />;
}

export function EditAdminKitPage() {
  const { id } = useParams<{ id: string }>();
  return <KitFormPage kitId={id} />;
}

function KitFormPage({ kitId }: { kitId?: string }) {
  const editing = kitId !== undefined;
  const { user } = useAuth();
  const [name, setName] = useState("");
  const [color, setColor] = useState("");
  const [price, setPrice] = useState("");
  const [components, setComponents] = useState<ComponentDraft[]>([
    emptyComponent(),
  ]);
  const [created, setCreated] = useState<KitSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(editing);

  useEffect(() => {
    if (!kitId) return;
    getKit(kitId)
      .then((kit) => {
        setName(kit.name);
        setColor(kit.color);
        setPrice(String(kit.price));
        setComponents(
          kit.components.map((component) => ({
            name: component.name,
            quantity: String(component.quantity),
            notes: component.notes ?? "",
          })),
        );
      })
      .catch(() => setError("Não foi possível carregar os dados do kit."))
      .finally(() => setLoading(false));
  }, [kitId]);

  function updateComponent(index: number, patch: Partial<ComponentDraft>) {
    setComponents((current) =>
      current.map((component, position) =>
        position === index ? { ...component, ...patch } : component,
      ),
    );
  }
  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    const body = {
      name: name.trim(),
      color: color.trim(),
      price: Number(price),
      components: components.map((component) => ({
        name: component.name.trim(),
        quantity: Number(component.quantity),
        notes: component.notes.trim() || null,
      })),
    };
    try {
      setCreated(kitId ? await updateKit(kitId, body) : await createKit(body));
    } catch {
      setError(
        `Não foi possível ${editing ? "salvar" : "cadastrar"} o kit. Confira os dados e se o nome já está em uso.`,
      );
    } finally {
      setBusy(false);
    }
  }
  if (!user?.permissions.includes(editing ? "KIT_UPDATE" : "KIT_CREATE"))
    return (
      <AppShell>
        <section className="workflow-card">
          <h1>Acesso não permitido</h1>
        </section>
      </AppShell>
    );
  return (
    <AppShell>
      <section className="workflow-card workflow-card--narrow">
        <p className="eyebrow">{editing ? "Edição" : "Novo cadastro"}</p>
        <h1>{editing ? "Editar kit" : "Cadastrar kit"}</h1>
        <p>
          O kit agrupa componentes (vidros, perfis, ferragens) com um preço de
          referência para orçamentos.
        </p>
        {created ? (
          <div className="workflow-feedback">
            <strong>
              {created.name} foi {editing ? "atualizado" : "cadastrado"}.
            </strong>
            <br />
            <Link to="/admin/kits">Ver kits</Link>
          </div>
        ) : loading ? (
          <p role="status">Carregando kit...</p>
        ) : (
          <form
            className="workflow-form"
            onSubmit={(event) => void submit(event)}
          >
            {error ? (
              <p className="workflow-feedback workflow-error" role="alert">
                {error}
              </p>
            ) : null}
            <label>
              Nome
              <input
                required
                maxLength={80}
                value={name}
                onChange={(event) => setName(event.target.value)}
              />
            </label>
            <label>
              Cor
              <input
                required
                maxLength={32}
                value={color}
                onChange={(event) => setColor(event.target.value)}
              />
            </label>
            <label>
              Preço (R$)
              <input
                type="number"
                required
                min={0}
                step="0.01"
                value={price}
                onChange={(event) => setPrice(event.target.value)}
              />
            </label>
            <fieldset>
              <legend>Componentes</legend>
              {components.map((component, index) => (
                <div
                  className="profile-options"
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    gap: "1rem",
                  }}
                  key={index}
                >
                  <label
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      justifyContent: "flex-start",
                      alignItems: "flex-start",
                      gap: "0.5rem",
                    }}
                  >
                    Componente
                    <input
                      required
                      maxLength={120}
                      value={component.name}
                      onChange={(event) =>
                        updateComponent(index, { name: event.target.value })
                      }
                    />
                  </label>
                  <label
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      justifyContent: "flex-start",
                      alignItems: "flex-start",
                      gap: "0.5rem",
                    }}
                  >
                    Quantidade
                    <input
                      type="number"
                      required
                      min={1}
                      value={component.quantity}
                      onChange={(event) =>
                        updateComponent(index, { quantity: event.target.value })
                      }
                    />
                  </label>
                  <label
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      justifyContent: "flex-start",
                      alignItems: "flex-start",
                      gap: "0.5rem",
                    }}
                  >
                    Observação
                    <input
                      maxLength={300}
                      value={component.notes}
                      onChange={(event) =>
                        updateComponent(index, { notes: event.target.value })
                      }
                    />
                  </label>
                  <button
                    className="button button--secondary"
                    type="button"
                    disabled={components.length === 1}
                    onClick={() =>
                      setComponents((current) =>
                        current.filter((_, position) => position !== index),
                      )
                    }
                  >
                    Remover
                  </button>
                </div>
              ))}
              <button
                className="button button--secondary"
                type="button"
                onClick={() =>
                  setComponents((current) => [...current, emptyComponent()])
                }
                style={{ width: "100%" }}
              >
                Adicionar componente
              </button>
            </fieldset>
            <button className="button button--primary" disabled={busy}>
              {editing
                ? busy
                  ? "Salvando..."
                  : "Salvar alterações"
                : busy
                  ? "Cadastrando..."
                  : "Cadastrar kit"}
            </button>
          </form>
        )}
      </section>
    </AppShell>
  );
}
