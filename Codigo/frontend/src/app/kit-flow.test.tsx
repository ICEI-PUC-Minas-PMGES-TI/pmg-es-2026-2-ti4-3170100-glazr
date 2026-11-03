import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, expect, it } from 'vitest'

import { server } from '../test/server'
import { App } from './App'

const kitsUser = {
  id: '1f5fb5d7-37ba-4e15-9d72-186da2db78d0',
  email: 'gestor@example.invalid',
  displayName: 'Gestor da Vidraçaria',
  active: true,
  profiles: ['ADMINISTRADOR'],
  permissions: ['KIT_READ', 'KIT_CREATE', 'KIT_UPDATE', 'KIT_DELETE'],
}

const existingKits = [
  {
    id: '2a3b4c5d-0000-0000-0000-000000000001',
    name: 'Kit Box Incolor',
    color: 'Incolor',
    price: 1250,
    components: [{ name: 'Vidro 8mm', quantity: 2, notes: null }],
    createdAt: '2026-09-14T00:00:00Z',
    updatedAt: '2026-09-14T00:00:00Z',
  },
  {
    id: '2a3b4c5d-0000-0000-0000-000000000002',
    name: 'Kit Porta Fumê',
    color: 'Fumê',
    price: 980.5,
    components: [
      { name: 'Vidro 6mm', quantity: 1, notes: null },
      { name: 'Ferragem', quantity: 1, notes: 'Inox' },
    ],
    createdAt: '2026-09-14T00:00:00Z',
    updatedAt: '2026-09-14T00:00:00Z',
  },
]

beforeEach(() => {
  window.history.replaceState({}, '', '/')
  server.use(
    http.get('http://localhost/api/v1/auth/me', () => HttpResponse.json(kitsUser)),
    http.get('http://localhost/api/v1/auth/csrf', () =>
      HttpResponse.json({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'csrf-test-token' }),
    ),
  )
})

it('lista kits com cor, preço e componentes para quem tem KIT_READ', async () => {
  server.use(http.get('http://localhost/api/v1/kits', () => HttpResponse.json({ content: existingKits })))
  window.history.replaceState({}, '', '/admin/kits')

  render(<App />)

  expect(await screen.findByText('Kit Box Incolor')).toBeInTheDocument()
  expect(screen.getByText('Kit Porta Fumê')).toBeInTheDocument()
  expect(screen.getByText('Fumê')).toBeInTheDocument()
  expect(screen.getByText('R$ 1.250,00')).toBeInTheDocument()
  expect(screen.getByText('2× Vidro 8mm')).toBeInTheDocument()
  expect(screen.getByText('1× Ferragem')).toBeInTheDocument()
})

it('cadastra kit com componentes enviando CSRF e mostra confirmação', async () => {
  let createdBody: Record<string, unknown> | undefined
  server.use(
    http.get('http://localhost/api/v1/kits', () => HttpResponse.json({ content: existingKits })),
    http.post('http://localhost/api/v1/kits', async ({ request }) => {
      expect(request.headers.get('X-XSRF-TOKEN')).toBe('csrf-test-token')
      createdBody = (await request.json()) as Record<string, unknown>
      return HttpResponse.json({
        id: '2a3b4c5d-0000-0000-0000-000000000003',
        name: 'Kit Box Bronze',
        color: 'Bronze',
        price: 1100,
        components: [{ name: 'Vidro 8mm', quantity: 3, notes: null }],
        createdAt: '2026-09-14T00:00:00Z',
        updatedAt: '2026-09-14T00:00:00Z',
      }, { status: 201 })
    }),
  )
  window.history.replaceState({}, '', '/admin/kits/new')

  const user = userEvent.setup()
  render(<App />)

  await user.type(await screen.findByLabelText('Nome'), 'Kit Box Bronze')
  await user.type(screen.getByLabelText('Cor'), 'Bronze')
  await user.type(screen.getByLabelText('Preço (R$)'), '1100')
  await user.type(screen.getByLabelText('Componente'), 'Vidro 8mm')
  const quantity = screen.getByLabelText('Quantidade')
  await user.clear(quantity)
  await user.type(quantity, '3')
  await user.click(screen.getByRole('button', { name: 'Cadastrar kit' }))

  expect(await screen.findByText('Kit Box Bronze foi cadastrado.')).toBeInTheDocument()
  expect(createdBody).toEqual({
    name: 'Kit Box Bronze',
    color: 'Bronze',
    price: 1100,
    components: [{ name: 'Vidro 8mm', quantity: 3, notes: null }],
  })
})

it('mostra erro quando o nome do kit já está em uso', async () => {
  server.use(
    http.get('http://localhost/api/v1/kits', () => HttpResponse.json({ content: existingKits })),
    http.post('http://localhost/api/v1/kits', () => HttpResponse.json({
      status: 409,
      title: 'Conflict',
      detail: 'Já existe um kit com este nome',
      code: 'DUPLICATE_KIT',
      instance: '/api/v1/kits',
      timestamp: '2026-09-14T00:00:00Z',
    }, { status: 409 })),
  )
  window.history.replaceState({}, '', '/admin/kits/new')

  const user = userEvent.setup()
  render(<App />)

  await user.type(await screen.findByLabelText('Nome'), 'Kit Box Incolor')
  await user.type(screen.getByLabelText('Cor'), 'Incolor')
  await user.type(screen.getByLabelText('Preço (R$)'), '900')
  await user.type(screen.getByLabelText('Componente'), 'Vidro 8mm')
  await user.click(screen.getByRole('button', { name: 'Cadastrar kit' }))

  expect(await screen.findByRole('alert')).toHaveTextContent(
    'Não foi possível cadastrar o kit. Confira os dados e se o nome já está em uso.')
})
it('edita kit existente carregando os dados e enviando PUT com CSRF', async () => {
  const kit = existingKits[1]
  let updatedBody: Record<string, unknown> | undefined
  server.use(
    http.get('http://localhost/api/v1/kits', () => HttpResponse.json({ content: existingKits })),
    http.get(`http://localhost/api/v1/kits/${kit.id}`, () => HttpResponse.json(kit)),
    http.put(`http://localhost/api/v1/kits/${kit.id}`, async ({ request }) => {
      expect(request.headers.get('X-XSRF-TOKEN')).toBe('csrf-test-token')
      updatedBody = (await request.json()) as Record<string, unknown>
      return HttpResponse.json({ ...kit, ...updatedBody })
    }),
  )
  window.history.replaceState({}, '', '/admin/kits')

  const user = userEvent.setup()
  render(<App />)

  await user.click(await screen.findByRole('button', { name: 'Ações para Kit Porta Fumê' }))
  await user.click(screen.getByRole('menuitem', { name: 'Editar' }))

  const price = await screen.findByLabelText('Preço (R$)')
  expect(screen.getByLabelText('Nome')).toHaveValue('Kit Porta Fumê')
  expect(screen.getAllByLabelText('Componente')).toHaveLength(2)
  await user.clear(price)
  await user.type(price, '1020')
  await user.click(screen.getByRole('button', { name: 'Salvar alterações' }))

  expect(await screen.findByText('Kit Porta Fumê foi atualizado.')).toBeInTheDocument()
  expect(updatedBody).toEqual({
    name: 'Kit Porta Fumê',
    color: 'Fumê',
    price: 1020,
    components: [
      { name: 'Vidro 6mm', quantity: 1, notes: null },
      { name: 'Ferragem', quantity: 1, notes: 'Inox' },
    ],
  })
})
