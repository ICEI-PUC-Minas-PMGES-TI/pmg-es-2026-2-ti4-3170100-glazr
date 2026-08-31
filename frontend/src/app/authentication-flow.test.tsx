import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, expect, it } from 'vitest'

import { server } from '../test/server'
import { App } from './App'

const authenticatedUser = {
  id: '1f5fb5d7-37ba-4e15-9d72-186da2db78d0',
  email: 'gestor@example.invalid',
  displayName: 'Gestor da Vidraçaria',
  active: true,
  profiles: ['ADMINISTRADOR'],
  permissions: ['USER_READ'],
}

const authenticationRequired = {
  status: 401,
  title: 'Unauthorized',
  detail: 'Autenticação necessária',
  code: 'AUTHENTICATION_REQUIRED',
  instance: '/api/v1/auth/me',
  timestamp: '2026-08-31T00:00:00Z',
}

beforeEach(() => window.history.replaceState({}, '', '/'))

it('entra com sessão e CSRF válidos', async () => {
  window.history.replaceState({}, '', '/login')
  server.use(
    http.get('http://localhost/api/v1/auth/me', () =>
      HttpResponse.json(authenticationRequired, { status: 401 }),
    ),
    http.get('http://localhost/api/v1/auth/csrf', () =>
      HttpResponse.json({
        headerName: 'X-XSRF-TOKEN',
        parameterName: '_csrf',
        token: 'csrf-test-token',
      }),
    ),
    http.post('http://localhost/api/v1/auth/login', async ({ request }) => {
      const credentials = (await request.json()) as Record<string, string>
      const validRequest =
        request.headers.get('X-XSRF-TOKEN') === 'csrf-test-token' &&
        credentials.email === 'gestor@example.invalid' &&
        credentials.password === 'senha-local-segura'
      return validRequest
        ? HttpResponse.json(authenticatedUser)
        : HttpResponse.json({ code: 'INVALID_CSRF_TOKEN', detail: 'Acesso negado' }, { status: 403 })
    }),
  )

  const user = userEvent.setup()
  render(<App />)

  await user.type(await screen.findByLabelText('E-mail'), 'gestor@example.invalid')
  await user.type(screen.getByLabelText('Senha'), 'senha-local-segura')
  await user.click(screen.getByRole('button', { name: 'Entrar' }))

  expect(await screen.findByText('Olá, Gestor da Vidraçaria')).toBeInTheDocument()
  expect(window.location.pathname).toBe('/dashboard')
})

it('mostra erro genérico e limpa a senha quando as credenciais são inválidas', async () => {
  window.history.replaceState({}, '', '/login')
  server.use(
    http.get('http://localhost/api/v1/auth/me', () =>
      HttpResponse.json(authenticationRequired, { status: 401 }),
    ),
    http.get('http://localhost/api/v1/auth/csrf', () =>
      HttpResponse.json({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'csrf-token' }),
    ),
    http.post('http://localhost/api/v1/auth/login', () =>
      HttpResponse.json(
        {
          status: 401,
          title: 'Unauthorized',
          detail: 'Credenciais inválidas',
          code: 'AUTHENTICATION_FAILED',
          instance: '/api/v1/auth/login',
          timestamp: '2026-08-31T00:00:00Z',
        },
        { status: 401 },
      ),
    ),
  )

  const user = userEvent.setup()
  render(<App />)

  await user.type(await screen.findByLabelText('E-mail'), 'desconhecido@example.invalid')
  const password = screen.getByLabelText('Senha')
  await user.type(password, 'senha-incorreta')
  await user.click(screen.getByRole('button', { name: 'Entrar' }))

  expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha inválidos.')
  expect(password).toHaveValue('')
})

it('redireciona visitante sem sessão para o login ao acessar rota protegida', async () => {
  window.history.replaceState({}, '', '/dashboard')
  server.use(
    http.get('http://localhost/api/v1/auth/me', () =>
      HttpResponse.json(authenticationRequired, { status: 401 }),
    ),
  )

  render(<App />)

  expect(await screen.findByRole('heading', { name: 'Acesse sua conta' })).toBeInTheDocument()
  await waitFor(() => expect(window.location.pathname).toBe('/login'))
})

it('restaura a sessão e encerra o acesso usando CSRF', async () => {
  window.history.replaceState({}, '', '/dashboard')
  server.use(
    http.get('http://localhost/api/v1/auth/me', () => HttpResponse.json(authenticatedUser)),
    http.get('http://localhost/api/v1/auth/csrf', () =>
      HttpResponse.json({
        headerName: 'X-XSRF-TOKEN',
        parameterName: '_csrf',
        token: 'logout-csrf-token',
      }),
    ),
    http.post('http://localhost/api/v1/auth/logout', ({ request }) =>
      request.headers.get('X-XSRF-TOKEN') === 'logout-csrf-token'
        ? new HttpResponse(null, { status: 204 })
        : HttpResponse.json({ code: 'INVALID_CSRF_TOKEN' }, { status: 403 }),
    ),
  )

  const user = userEvent.setup()
  render(<App />)

  expect(await screen.findByText('Olá, Gestor da Vidraçaria')).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Sair' }))

  expect(await screen.findByRole('heading', { name: 'Acesse sua conta' })).toBeInTheDocument()
  expect(window.location.pathname).toBe('/login')
})
