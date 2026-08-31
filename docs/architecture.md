# Arquitetura do Glazr

## Visão geral

O Glazr combina uma SPA React/TypeScript com um monólito modular Java 21/Spring Boot 3.5.16. O processo de aplicação contém autenticação, usuários, perfis e permissões, cada qual com fronteiras de pacote e responsabilidades próprias. PostgreSQL é a fonte de verdade para dados de negócio, catálogo, locks de autenticação e sessões.

```text
Browser
     |
     v
Nginx :8080 ----> SPA React (arquivos estáticos + fallback)
     |
     | /api/*, mesma origem, cookies + X-XSRF-TOKEN
     v
Spring Boot :8080 ----------------------+
     |                                  |
     | JPA/Flyway                       | Spring Session JDBC
     v                                  v
                 PostgreSQL :5432
```

Somente o Nginx é publicado normalmente no host. Ele serve a SPA e encaminha a API; backend e banco comunicam-se pela rede interna do Compose. Em desenvolvimento, o backend também envia convites e redefinições ao Mailpit pela rede interna. Em produção, o mesmo adapter usa SMTP externo configurado por segredo.

## Frontend

O cliente é dividido por responsabilidade:

- `app`: providers, router e guards de rota;
- `pages`: composição das telas de login e área inicial;
- `features/auth`: contratos, API, estado em memória, hooks e componentes da autenticação;
- `features/users`: administração de funcionários e convites;
- `features/invitations`: captura segura do fragmento e ativação;
- `features/account-security`: senha, reset e vínculo Google;
- `shared`: cliente HTTP, Problem Details, componentes genéricos e design tokens.

O cliente HTTP é a única fronteira para `fetch`, aplica `credentials: include`, interpreta JSON, `204` e `application/problem+json`. O módulo de autenticação obtém CSRF imediatamente antes de login e logout. O `AuthProvider` modela `loading`, `authenticated` e `unauthenticated`; a sessão é reconstruída por `/auth/me` e nunca armazenada no browser.

## Módulos

| Módulo | Responsabilidade | Dependências principais |
| --- | --- | --- |
| `authentication` | Login, logout, usuário atual, bloqueio e principal autenticado | `user`, Spring Security/Session |
| `user` | Ciclo administrativo do usuário, perfis associados e regra do último administrador | `accessprofile`, `shared` |
| `invitation` | Convites temporários, ativação e entrega pós-commit | `user`, `credential`, `mail`, `shared` |
| `credential` | Senhas locais, resets e identidades externas | `user`, `authentication`, `shared` |
| `federatedidentity` | Claims Google, vínculo por `sub` e callback OIDC | `credential`, `user`, Spring OAuth2 Client |
| `mail` | Porta de mensagens e adapter SMTP texto/HTML | Spring Mail |
| `accessprofile` | CRUD controlado de perfis e proteção de `ADMINISTRADOR` | `permission`, `shared` |
| `permission` | Catálogo fixo e consulta somente leitura | `shared` |
| `configuration` | Security chain, cookies, CORS, relógio e propriedades | módulos funcionais |
| `shared` | Problem Details, exceções de domínio e tipos HTTP comuns | Spring Web apenas na camada API |

Dentro de cada módulo:

- `api`: DTOs, controllers e mapeamento HTTP;
- `application`: casos de uso transacionais e portas necessárias;
- `domain`: invariantes e tipos centrais;
- `infrastructure`: JPA, adapters e integração com frameworks.

Controllers nunca usam repositories diretamente. A API retorna records DTO, não entidades JPA.

## Identidade e credenciais

```text
                         +-> user_password_credentials
users (fonte de verdade) +-> user_external_identities (GOOGLE/sub)
   |                     +-> employee_invitations
   +------------------------> password_reset_requests
```

`users` mantém estado, tipo de conta, nome, e-mail e perfis. Credenciais são relações opcionais; um funcionário pode possuir senha, Google ou ambos. Todos os métodos constroem `GlazrUserPrincipal` a partir do mesmo usuário local e terminam em Spring Session JDBC. Perfis e permissions nunca são obtidos do Google.

O fluxo local consulta a credencial de senha pelo UUID do usuário e preserva o bloqueio por tentativas. O fluxo Google procura primeiro `(provider, subject)`; um primeiro vínculo só é permitido quando já existe um `EMPLOYEE` com e-mail verificado idêntico. Constraints únicas impedem vínculos concorrentes ou cruzados.

## Convites e tokens temporários

Convites e resets usam tokens de 256 bits, entregues em fragmentos de URL e armazenados somente como SHA-256. O frontend remove o fragmento imediatamente e envia o token apenas no corpo de uma requisição HTTPS. Consumo, revogação e expiração são validados sob lock; a atualização da credencial e o consumo pertencem à mesma transação.

```text
admin cria funcionário
  -> transação: user + invitation(PENDING)
  -> after-commit: EmployeeMailSender
       -> SMTP aceitou: SENT
       -> falhou: FAILED, usuário preservado
  -> funcionário ativa por senha ou Google
       -> credencial vinculada ao mesmo user.id
       -> invitation.consumed_at
       -> sessão Glazr
```

Reenvio revoga o convite anterior e cria token novo. O envio externo ocorre fora da transação de negócio para não prender conexão nem desfazer a criação do usuário por indisponibilidade SMTP.

## Google OIDC

Spring Security OAuth2 Client controla authorization code, `state`, `nonce`, assinatura, issuer, audience e expiração. O mapper da aplicação exige `email_verified=true`, normaliza o e-mail e usa `sub` como chave permanente. O token do convite nunca é enviado ao Google: a sessão curta guarda somente o UUID interno do convite.

Tokens do Google existem somente durante o callback; não são persistidos, enviados à SPA ou usados como sessão do Glazr. Após o vínculo, o backend cria o mesmo `SecurityContext` e cookie `GLAZR_SESSION` do login local. Com Google desabilitado, nenhum endpoint ou botão do provedor fica disponível.

## Fluxos críticos

### Inicialização

1. Flyway cria e valida schema, tabelas de sessão, catálogo e perfil protegido.
2. Hibernate valida o schema sem alterá-lo.
3. O bootstrap bloqueia pessimisticamente a linha do perfil `ADMINISTRADOR` e consulta a existência de usuários.
4. Banco vazio materializa e exige as três variáveis `GLAZR_BOOTSTRAP_ADMIN_*`; cria a primeira conta com SHA-256/Base64 + BCrypt 12 e associa `ADMINISTRADOR`.
5. Banco já inicializado ignora as variáveis sem acessar ou registrar a senha.

### Login

1. A SPA obtém CSRF e mantém o token apenas durante a mutação.
2. Login normaliza apenas o e-mail e lê o hash sem manter uma transação longa.
3. Senha é pré-processada por SHA-256/Base64 e comparada por BCrypt fora do lock; usuário desconhecido também executa comparação com hash sentinela.
4. Uma segunda etapa transacional curta relê e bloqueia a conta pelo UUID observado, confirma que e-mail/hash não mudaram, revalida estado e registra a falha ou sucesso atomicamente.
5. Sucesso zera falhas, atualiza último login, troca o ID da sessão, salva o `SecurityContext` e retorna DTO seguro.

### Revalidação da sessão

```text
cookie GLAZR_SESSION
  -> Spring Session carrega contexto
  -> filtro consulta usuário e perfis atuais
     -> inativo/ausente: invalida + 401
     -> ativo: recompõe authorities
  -> @PreAuthorize verifica permissão
  -> controller delega ao caso de uso
```

Essa revalidação impede que desativação ou remoção de uma permissão mantenha acesso até o timeout. A desativação também remove as sessões indexadas pelo nome principal.

### Último administrador

Operações que desativam uma conta ou removem o perfil protegido bloqueiam pessimisticamente/serializam a verificação aplicável e contam usuários ativos associados a `ADMINISTRADOR`. A transação só prossegue quando restará ao menos um. O perfil protegido não pode ser alterado ou excluído pela API.

## Segurança

- Sessão no servidor em PostgreSQL; não há JWT.
- Cookie `GLAZR_SESSION` HttpOnly, SameSite=Lax, Path=/ e Secure em produção.
- CSRF por double-submit cookie gerenciado pelo Spring Security; mutações exigem `X-XSRF-TOKEN`.
- CORS desabilitado em produção por padrão e limitado a allowlist explícita em desenvolvimento.
- Formato versionado `{bcrypt-sha256}`: SHA-256/Base64 determinístico antes de BCrypt força 12 evita o limite de entrada de 72 bytes do BCrypt; senha entre 12 e 128 caracteres, sem regras artificiais nem normalização.
- Session fixation mitigada por troca do ID no login.
- Erros de login são indistinguíveis e seguros.
- Tokens de convite/reset são hash-only, de uso único e nunca entram em logs.
- Tokens OAuth não substituem a sessão local nem são persistidos.
- Resets usam resposta genérica e invalidam sessões anteriores.
- Permissões são authorities e verificadas por `@PreAuthorize`.
- Apenas `/actuator/health` é exposto e seus detalhes não são públicos.

## Persistência e concorrência

Flyway é o único proprietário do DDL. Índices funcionais em `lower(email)` e `lower(name)` são a barreira definitiva contra duplicidade. Chaves compostas evitam associações repetidas. `@Version` detecta atualizações concorrentes em usuários e perfis. Transações curtas com locks pessimistas serializam somente a gravação de tentativas de login, sem manter conexão presa durante BCrypt; a linha do perfil `ADMINISTRADOR` funciona como mutex global para bootstrap e proteção do último administrador. Violações de constraint são traduzidas para conflitos estáveis da API.

Aplicação e banco usam UTC. UUIDs são gerados pela aplicação antes do `persist`, o que mantém identidade estável nos casos de uso e testes.

## Operação

O Dockerfile do backend compila pelo wrapper em JDK 21 e executa como usuário não root em JRE 21. O Dockerfile do Nginx usa Node 24.15 em uma etapa de build reproduzível com `npm ci`, copia somente o `dist` para a imagem final e configura fallback da SPA sem interferir em `/api/` ou `/health`. Compose fixa imagens, mantém o banco em volume nomeado e condiciona o backend à saúde do PostgreSQL. Nginx limita corpo, configura timeouts, oculta sua versão e preserva headers do proxy.

O Compose de desenvolvimento adiciona Mailpit com versão fixa, SMTP interno e interface web local. Produção não depende de Mailpit e recebe configuração SMTP/Google exclusivamente por variáveis e secrets.

Os profiles Spring separam defaults locais, teste controlado e produção segura. Nenhum profile de produção possui senha padrão. `.env.example` contém apenas valores ilustrativos e é proibido versionar `.env`.

## Decisões deliberadas

- Revalidação por requisição foi escolhida em vez de guardar authorities imutáveis por oito horas.
- Exclusão de sessões por principal complementa, mas não substitui, a revalidação.
- O catálogo e o perfil protegido são dados de sistema versionados por migration; a senha inicial nunca entra em migration.
- O update de perfil usa `PUT`, pois substitui seu estado administrativo e o conjunto de permissões de forma explícita.
- Não há abstrações para multiempresa, mensageria, estado global genérico ou biblioteca visual enquanto não existir requisito.
- Conta local é a identidade; métodos de autenticação são credenciais substituíveis e nunca originam permissões.
- Clientes, orçamento e marketing permanecem módulos futuros sem schema ou telas antecipadas nesta fase.
