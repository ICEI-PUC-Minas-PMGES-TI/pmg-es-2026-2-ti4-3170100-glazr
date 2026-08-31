# Frontend Authentication Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar uma SPA React/TypeScript com login por sessão e CSRF, restauração de sessão, rota protegida, logout e execução pelo Nginx do Docker Compose.

**Architecture:** O frontend é organizado por aplicação, páginas, feature de autenticação e recursos compartilhados. O Nginx serve o build estático e mantém `/api/` como proxy de mesma origem para o Spring Boot; o estado autenticado existe somente em memória e é reconstituído por `/auth/me`.

**Tech Stack:** React 19, TypeScript strict, Vite, React Router, Vitest, Testing Library, MSW, ESLint, CSS e Docker multi-stage com Node e Nginx.

**Spec:** `docs/specs/frontend-authentication-foundation.md`

## Global Constraints

- Toda a interface é em português do Brasil.
- Use somente URLs relativas sob `/api/v1` e `credentials: "include"`.
- Não use JWT, localStorage, sessionStorage, credenciais de exemplo reais ou bibliotecas visuais pesadas.
- Use a paleta `#1E3A5F`, `#2F557F`, `#7FA6C9`, `#EAF2F8`, `#FFFFFF` e `#172A3A` como tokens CSS.
- A suíte terá somente quatro testes comportamentais de alto valor.
- O backend Java não será alterado salvo incompatibilidade comprovada.

---

### Task 1: Scaffold reproduzível e cliente HTTP

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/package-lock.json`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tsconfig.app.json`
- Create: `frontend/tsconfig.node.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/eslint.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/vite-env.d.ts`
- Create: `frontend/src/shared/api/api-error.ts`
- Create: `frontend/src/shared/api/http-client.ts`
- Create: `frontend/src/shared/api/http-client.test.ts`
- Create: `frontend/src/test/setup.ts`

**Interfaces:**
- Produces: `requestJson<T>(path: string, init?: RequestInit): Promise<T>` e `ApiError` com `status`, `code` e `detail`.

- [ ] Instalar dependências com versões estáveis exatas e gerar lockfile com `npm install --save-exact`.
- [ ] Configurar TypeScript strict, scripts `dev`, `build`, `lint`, `typecheck`, `test` e proxy Vite de `/api` para `http://localhost:8080`.
- [ ] Escrever teste que força `requestJson` a transformar `application/problem+json` em `ApiError` e verificar a falha inicial.
- [ ] Implementar o cliente mínimo com `credentials: "include"`, parsing seguro de JSON/204 e erro de rede amigável.
- [ ] Rodar o teste focal, typecheck e lint até ficarem verdes.
- [ ] Commit: `chore(frontend): scaffold React TypeScript application`.

### Task 2: API e estado de autenticação

**Files:**
- Create: `frontend/src/features/auth/model/auth-types.ts`
- Create: `frontend/src/features/auth/api/auth-api.ts`
- Create: `frontend/src/features/auth/model/auth-context.ts`
- Create: `frontend/src/features/auth/model/AuthProvider.tsx`
- Create: `frontend/src/features/auth/hooks/use-auth.ts`
- Create: `frontend/src/features/auth/model/AuthProvider.test.tsx`

**Interfaces:**
- Consumes: `requestJson<T>` e `ApiError`.
- Produces: `getCurrentUser()`, `login(credentials)`, `logout()`, `AuthProvider` e `useAuth()` com `status`, `user`, `login`, `logout` e estado de operação.

- [ ] Escrever teste de restauração que responde `/me` com usuário completo e exige estado autenticado após o loading; executar para confirmar RED.
- [ ] Definir contratos exatos de `CsrfResponse`, `LoginCredentials`, `AuthenticatedUser` e `ProblemDetail`.
- [ ] Implementar chamadas de CSRF, login, `/me` e logout, obtendo CSRF antes de cada POST.
- [ ] Implementar provider com inicialização única, estados explícitos e limpeza da sessão em `401`.
- [ ] Executar o teste focal para confirmar GREEN e refatorar apenas após a passagem.
- [ ] Manter tokens CSRF somente no escopo da mutação e nunca persistir dados sensíveis.

### Task 3: Login, guard e página autenticada

**Files:**
- Create: `frontend/src/app/App.tsx`
- Create: `frontend/src/app/router.tsx`
- Create: `frontend/src/app/ProtectedRoute.tsx`
- Create: `frontend/src/app/PublicOnlyRoute.tsx`
- Create: `frontend/src/pages/LoginPage.tsx`
- Create: `frontend/src/pages/HomePage.tsx`
- Create: `frontend/src/features/auth/components/LoginForm.tsx`
- Create: `frontend/src/features/auth/components/UserSummary.tsx`
- Create: `frontend/src/shared/components/AppLoading.tsx`
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/app/authentication-flow.test.tsx`

**Interfaces:**
- Consumes: `AuthProvider` e `useAuth()`.
- Produces: rotas `/login` e `/`, formulário acessível e área autenticada.

- [ ] Escrever teste do login que exige GET de CSRF, POST com header recebido, saudação após sucesso e senha limpa; confirmar RED.
- [ ] Escrever teste de credenciais inválidas que exige mensagem genérica; confirmar RED.
- [ ] Escrever teste de rota protegida e logout que exige redirecionamento e POST com CSRF; confirmar RED.
- [ ] Implementar guardas, páginas e componentes mínimos para deixar os quatro testes verdes.
- [ ] Traduzir códigos esperados sem expor detalhes internos e impedir submissões repetidas.
- [ ] Rodar os quatro testes, typecheck e lint; revisar hooks, acessibilidade, imports e renderizações conforme a skill de boas práticas React.
- [ ] Commit: `feat(frontend): implement session authentication flow`.

### Task 4: Estilos, build de produção e Compose

**Files:**
- Create: `frontend/src/shared/styles/tokens.css`
- Create: `frontend/src/shared/styles/global.css`
- Create: `frontend/src/pages/LoginPage.module.css`
- Create: `frontend/src/pages/HomePage.module.css`
- Create: `frontend/src/features/auth/components/LoginForm.module.css`
- Create: `frontend/src/features/auth/components/UserSummary.module.css`
- Create: `frontend/.dockerignore`
- Create: `infra/nginx/Dockerfile`
- Modify: `infra/nginx/nginx.conf`
- Modify: `compose.yaml`
- Modify: `.gitignore`

**Interfaces:**
- Produces: frontend responsivo servido em `/`, SPA fallback e `/api/` preservado.

- [ ] Implementar tokens, reset, layout responsivo, foco visível, `aria-live` e redução de movimento.
- [ ] Configurar Dockerfile multi-stage com imagens fixadas: Node compila `frontend`, Nginx recebe somente `dist` e configuração.
- [ ] Alterar o serviço `nginx` para buildar a imagem sem expor portas adicionais; manter healthcheck e dependência do backend saudável.
- [ ] Configurar `try_files $uri $uri/ /index.html` somente na localização da SPA e preservar `/health` e `/api/`.
- [ ] Rodar `npm run build` e `docker compose config`; inspecionar imagem e contexto para artefatos ou segredos.
- [ ] Fazer verificação visual em desktop e viewport móvel, incluindo ausência de overlay e conteúdo não vazio.
- [ ] Commit: `feat(infra): serve frontend through Docker and Nginx`.

### Task 5: Documentação, verificação final e entrega

**Files:**
- Modify: `README.md`
- Modify: `docs/architecture.md`

**Interfaces:**
- Produces: instruções operacionais e descrição arquitetural atualizadas.

- [ ] Atualizar visão geral, árvore, execução Docker, URL, autenticação, comandos frontend e limitações.
- [ ] Atualizar o diagrama e as responsabilidades arquiteturais do cliente e do Nginx.
- [ ] Executar `npm ci`, `npm run lint`, `npm run typecheck`, `npm test -- --run` e `npm run build` em estado limpo.
- [ ] Executar `docker compose --env-file .env config`, build, `up -d --wait`, healthchecks e smoke de rotas sem imprimir credenciais.
- [ ] Revisar o diff completo e staged, confirmar ausência de `.env`, chaves, tokens, senhas, `node_modules`, `dist` e cobertura.
- [ ] Commit: `docs: document frontend authentication workflow`.
- [ ] Sincronizar com `origin/main`, integrar sem force-push, repetir verificações relevantes e enviar para `origin/main`.
