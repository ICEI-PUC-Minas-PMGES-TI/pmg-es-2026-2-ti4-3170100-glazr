import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, expect, it } from 'vitest'

import { server } from '../test/server'
import { App } from './App'

const adminUser = {
  id: '1f5fb5d7-37ba-4e15-9d72-186da2db78d0',
  email: 'admin@glazr.local',
  displayName: 'Administrador do Sistema',
  active: true,
  profiles: ['ADMINISTRADOR'],
  permissions: ['USER_READ', 'USER_CREATE', 'USER_UPDATE', 'USER_STATUS_UPDATE', 'ACCESS_PROFILE_ASSIGN'],
}

const mockProfiles = {
  content: [
    { id: '00000000-0000-0000-0000-000000000001', name: 'ADMINISTRADOR', active: true },
    { id: '00000000-0000-0000-0000-000000000002', name: 'VENDEDOR', active: true },
  ],
}

const mockUsers = [
  {
    id: '1f5fb5d7-37ba-4e15-9d72-186da2db78d0',
    email: 'admin@glazr.local',
    displayName: 'Administrador do Sistema',
    active: true,
    accountType: 'EMPLOYEE',
    authenticationMethods: ['PASSWORD'],
    invitationStatus: null,
    invitationExpiresAt: null,
    invitationDeliveryStatus: null,
    accessProfiles: [{ id: '00000000-0000-0000-0000-000000000001', name: 'ADMINISTRADOR' }],
  },
  {
    id: '2f5fb5d7-37ba-4e15-9d72-186da2db78d1',
    email: 'vendedor@glazr.local',
    displayName: 'Vendedor Exemplo',
    active: true,
    accountType: 'EMPLOYEE',
    authenticationMethods: [],
    invitationStatus: 'PENDING',
    invitationExpiresAt: '2026-09-20T12:00:00Z',
    invitationDeliveryStatus: 'SENT',
    accessProfiles: [{ id: '00000000-0000-0000-0000-000000000002', name: 'VENDEDOR' }],
  },
  {
    id: '3f5fb5d7-37ba-4e15-9d72-186da2db78d2',
    email: 'inativo@glazr.local',
    displayName: 'Funcionario Inativo',
    active: false,
    accountType: 'EMPLOYEE',
    authenticationMethods: ['PASSWORD'],
    invitationStatus: null,
    invitationExpiresAt: null,
    invitationDeliveryStatus: null,
    accessProfiles: [{ id: '00000000-0000-0000-0000-000000000002', name: 'VENDEDOR' }],
  },
]

beforeEach(() => {
  window.history.replaceState({}, '', '/')
  server.use(
    http.get('http://localhost/api/v1/auth/me', () => HttpResponse.json(adminUser)),
    http.get('http://localhost/api/v1/auth/csrf', () =>
      HttpResponse.json({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'csrf-test-token' }),
    ),
    http.get('http://localhost/api/v1/users', () => HttpResponse.json({ content: mockUsers })),
    http.get('http://localhost/api/v1/access-profiles', () => HttpResponse.json(mockProfiles)),
  )
})

it('lista funcionários com contadores e permite busca e filtros', async () => {
  window.history.replaceState({}, '', '/admin/users')
  const user = userEvent.setup()
  render(<App />)

  // Verifica carregamento dos itens
  expect(await screen.findByText('Administrador do Sistema')).toBeInTheDocument()
  expect(screen.getByText('Vendedor Exemplo')).toBeInTheDocument()
  expect(screen.getByText('Funcionario Inativo')).toBeInTheDocument()

  // Verifica contadores
  expect(screen.getByText(/Total:/)).toHaveTextContent('Total: 3 (2 ativos, 1 inativos)')

  // Testa busca por nome
  const searchInput = screen.getByPlaceholderText('Buscar por nome ou e-mail...')
  await user.type(searchInput, 'vendedor')
  expect(screen.getByText('Vendedor Exemplo')).toBeInTheDocument()
  expect(screen.queryByText('Administrador do Sistema')).not.toBeInTheDocument()

  // Limpa busca
  await user.clear(searchInput)

  // Testa filtro por status inativo
  const statusSelect = screen.getByLabelText('Filtrar por status')
  await user.selectOptions(statusSelect, 'INACTIVE')
  expect(screen.getByText('Funcionario Inativo')).toBeInTheDocument()
  expect(screen.queryByText('Vendedor Exemplo')).not.toBeInTheDocument()
})

it('cadastra novo funcionário com perfil selecionado via cartões', async () => {
  let createdBody: Record<string, unknown> | undefined
  server.use(
    http.post('http://localhost/api/v1/users', async ({ request }) => {
      expect(request.headers.get('X-XSRF-TOKEN')).toBe('csrf-test-token')
      createdBody = (await request.json()) as Record<string, unknown>
      return HttpResponse.json(
        {
          id: '4f5fb5d7-37ba-4e15-9d72-186da2db78d3',
          email: 'novo@glazr.local',
          displayName: 'Novo Funcionario',
          active: true,
          accountType: 'EMPLOYEE',
          authenticationMethods: [],
          invitationStatus: 'PENDING',
          invitationExpiresAt: '2026-09-20T12:00:00Z',
          invitationDeliveryStatus: 'SENT',
          accessProfiles: [{ id: '00000000-0000-0000-0000-000000000002', name: 'VENDEDOR' }],
        },
        { status: 201 },
      )
    }),
  )

  window.history.replaceState({}, '', '/admin/users/new')
  const user = userEvent.setup()
  render(<App />)

  await user.type(await screen.findByLabelText('Nome completo'), 'Novo Funcionario')
  await user.type(screen.getByLabelText('E-mail corporativo'), 'novo@glazr.local')
  await user.click(screen.getByRole('button', { name: 'Criar e enviar convite' }))

  expect(await screen.findByText(/Convite enviado com sucesso para Novo Funcionario!/)).toBeInTheDocument()
  expect(createdBody).toEqual({
    displayName: 'Novo Funcionario',
    email: 'novo@glazr.local',
    accessProfileIds: ['00000000-0000-0000-0000-000000000002'],
  })
})

it('edita dados e trata proteção contra desativação do último administrador', async () => {
  server.use(
    http.get('http://localhost/api/v1/users/1f5fb5d7-37ba-4e15-9d72-186da2db78d0', () =>
      HttpResponse.json(mockUsers[0]),
    ),
    http.patch('http://localhost/api/v1/users/1f5fb5d7-37ba-4e15-9d72-186da2db78d0', () =>
      HttpResponse.json(mockUsers[0]),
    ),
    http.patch('http://localhost/api/v1/users/1f5fb5d7-37ba-4e15-9d72-186da2db78d0/status', () =>
      HttpResponse.json(
        {
          status: 409,
          title: 'Conflict',
          detail: 'Não é possível desativar o último administrador ativo',
          code: 'LAST_ACTIVE_ADMINISTRATOR',
          instance: '/api/v1/users/1f5fb5d7-37ba-4e15-9d72-186da2db78d0/status',
          timestamp: '2026-09-14T00:00:00Z',
        },
        { status: 409 },
      ),
    ),
  )

  window.history.replaceState({}, '', '/admin/users/1f5fb5d7-37ba-4e15-9d72-186da2db78d0/edit')
  const user = userEvent.setup()
  render(<App />)

  // Aguarda carregamento
  expect(await screen.findByDisplayValue('Administrador do Sistema')).toBeInTheDocument()

  // Altera status para Inativo
  const statusSelect = screen.getByLabelText('Status do acesso')
  await user.selectOptions(statusSelect, 'false')

  await user.click(screen.getByRole('button', { name: 'Salvar alterações' }))

  expect(
    await screen.findByText('Não é possível inativar ou remover o perfil do último administrador ativo do sistema.'),
  ).toBeInTheDocument()
})

it('agrupa as ações de cada funcionário em um menu', async () => {
  let resendCalled = false
  server.use(
    http.post('http://localhost/api/v1/users/2f5fb5d7-37ba-4e15-9d72-186da2db78d1/invitation/resend', () => {
      resendCalled = true
      return new HttpResponse(null, { status: 204 })
    }),
  )
  window.history.replaceState({}, '', '/admin/users')
  const user = userEvent.setup()
  render(<App />)

  await user.click(await screen.findByRole('button', { name: 'Ações para Administrador do Sistema' }))
  expect(screen.getByRole('menuitem', { name: 'Editar' })).toHaveFocus()
  expect(screen.getByRole('menuitem', { name: 'Inativar' })).toBeInTheDocument()
  expect(screen.queryByRole('menuitem', { name: 'Reenviar convite' })).not.toBeInTheDocument()

  await user.keyboard('{Escape}')
  expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Ações para Administrador do Sistema' })).toHaveFocus()

  await user.click(screen.getByRole('button', { name: 'Ações para Vendedor Exemplo' }))
  expect(screen.getByRole('menuitem', { name: 'Revogar convite' })).toBeInTheDocument()
  await user.click(screen.getByRole('menuitem', { name: 'Reenviar convite' }))

  expect(await screen.findByText('Convite reenviado para vendedor@glazr.local.')).toBeInTheDocument()
  expect(resendCalled).toBe(true)
  expect(screen.queryByRole('menu')).not.toBeInTheDocument()
})

it('abre a edição do funcionário pelo menu de ações', async () => {
  server.use(
    http.get('http://localhost/api/v1/users/3f5fb5d7-37ba-4e15-9d72-186da2db78d2', () => HttpResponse.json(mockUsers[2])),
  )
  window.history.replaceState({}, '', '/admin/users')
  const user = userEvent.setup()
  render(<App />)

  await user.click(await screen.findByRole('button', { name: 'Ações para Funcionario Inativo' }))
  await user.click(screen.getByRole('menuitem', { name: 'Editar' }))

  expect(await screen.findByRole('heading', { name: 'Editar funcionário' })).toBeInTheDocument()
  expect(window.location.pathname).toBe('/admin/users/3f5fb5d7-37ba-4e15-9d72-186da2db78d2/edit')
})
