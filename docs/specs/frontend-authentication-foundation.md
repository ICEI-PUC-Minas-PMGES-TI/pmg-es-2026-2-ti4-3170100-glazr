# Fundação do frontend e autenticação

## Objetivo

Entregar uma SPA React/TypeScript enxuta para autenticação real por sessão, servida pelo Nginx na mesma origem da API. A etapa inclui login, restauração da sessão, rota protegida, visão inicial do usuário e logout, sem módulos administrativos ou recuperação de senha.

## Arquitetura

O frontend vive em `frontend/` e separa composição da aplicação, páginas, domínio de autenticação e recursos compartilhados. Um cliente HTTP central interpreta respostas JSON, vazias e `application/problem+json`. O módulo de autenticação encapsula CSRF, login, usuário atual e logout; o provider expõe somente estado e ações para a interface.

```text
Browser
  -> Nginx :8080
       -> arquivos estáticos da SPA
       -> /api/* -> Spring Boot :8080
  -> cookie HttpOnly GLAZR_SESSION
  -> header CSRF obtido em /api/v1/auth/csrf
```

O build de produção será multi-stage: Node compila o frontend e a imagem final do Nginx recebe apenas `dist`, a configuração do gateway e arquivos estáticos. PostgreSQL e backend permanecem internos ao Compose.

## Estrutura

```text
frontend/src/
  app/                 router, provider e guards
  pages/               LoginPage e HomePage
  features/auth/
    api/               contrato HTTP de autenticação
    components/        formulário de login e visão do usuário
    hooks/             acesso tipado ao contexto
    model/             tipos, reducer/provider e mensagens
  shared/
    api/               cliente HTTP e Problem Details
    components/        estado de carregamento
    styles/            reset, tokens e estilos globais
    types/             tipos compartilhados necessários
```

## Fluxo de sessão

Na inicialização, o provider consulta `GET /api/v1/auth/me` uma única vez e mantém `loading` até a resposta. `401` estabelece estado anônimo sem alerta técnico; sucesso estabelece o usuário autenticado. O guard envia visitantes para `/login` e a rota de login envia usuários autenticados para `/`.

Login e logout obtêm um token por `GET /api/v1/auth/csrf` imediatamente antes da mutação. O header é definido pelo `headerName` recebido, todas as requisições usam `credentials: "include"` e nenhuma credencial ou token é persistido no navegador. Login bem-sucedido usa o usuário retornado pela própria API. Logout trata `204` como sucesso e limpa o estado local; uma sessão já expirada também termina no login.

O cliente HTTP converte Problem Details em `ApiError`. A interface traduz códigos esperados para mensagens seguras em português. `AUTHENTICATION_FAILED` nunca diferencia conta inexistente, inativa ou bloqueada. Erros de rede recebem mensagem separada e `401` em chamadas autenticadas remove a sessão local.

## Interface

A tela de login usa card central, marca textual Glazr, labels visíveis, `autocomplete` correto, botão com estado de envio e região acessível para erro. O layout usa tokens com azul profundo `#1E3A5F`, principal `#2F557F`, intermediário `#7FA6C9`, claro `#EAF2F8`, branco e texto `#172A3A`. Gradientes e formas são discretos e produzidos somente com CSS.

A página autenticada apresenta saudação, e-mail, perfis, permissões de forma secundária e ação de logout. Ambas as telas são responsivas, operáveis por teclado, respeitam redução de movimento e não dependem de imagens ou fontes externas.

## Segurança

- não há JWT ou autenticação simulada;
- senha, cookie, sessão e CSRF não entram em storage nem logs;
- o cookie `GLAZR_SESSION` permanece opaco e HttpOnly;
- o frontend usa somente URLs relativas `/api/v1`;
- o campo de senha é limpo após cada tentativa;
- `.env`, `node_modules`, `dist` e cobertura não são versionados;
- respostas técnicas inesperadas não são expostas diretamente ao usuário.

## Testes

A suíte terá poucos testes de alto valor com Vitest, Testing Library e MSW: fluxo de login e CSRF, credenciais inválidas, proteção/restauração de sessão e logout. Os testes observam comportamento da interface e contratos HTTP, sem metas de cobertura nem assertions sobre detalhes internos.

## Fora do escopo

Cadastro público, alteração ou recuperação de senha, MFA, OAuth/SSO, CRUD administrativo, módulos comerciais, biblioteca visual pesada e persistência de estado de autenticação no cliente permanecem fora desta entrega.
