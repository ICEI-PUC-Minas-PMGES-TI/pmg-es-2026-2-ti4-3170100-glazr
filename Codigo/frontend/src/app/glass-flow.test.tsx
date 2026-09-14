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
  permissions: ['GLASS_READ', 'GLASS_CREATE', 'GLASS_UPDATE', 'GLASS_DELETE'],
}

const mockGlasses = [
  {
    id: 'a1111111-1111-1111-1111-111111111111',
    name: 'Vidro Temperado Incolor',
    thicknessMm: 8,
    finish: 'Lapidado',
    color: 'Incolor',
    pricePerSquareMeter: 180.0,
    active: true,
    createdAt: '2026-09-14T10:00:00Z',
    updatedAt: '2026-09-14T10:00:00Z',
  },
  {
    id: 'a2222222-2222-2222-2222-222222222222',
    name: 'Vidro Laminado Fumê',
    thicknessMm: 10,
    finish: 'Bisotê',
    color: 'Fumê',
    pricePerSquareMeter: 240.5,
    active: true,
    createdAt: '2026-09-14T10:00:00Z',
    updatedAt: '2026-09-14T10:00:00Z',
  },
  {
    id: 'a3333333-3333-3333-3333-333333333333',
    name: 'Vidro Comum Verde',
    thicknessMm: 4,
    finish: 'Sem acabamento',
    color: 'Verde',
    pricePerSquareMeter: 75.0,
    active: false,
    createdAt: '2026-09-14T10:00:00Z',
    updatedAt: '2026-09-14T10:00:00Z',
  },
]

beforeEach(() => {
  window.history.replaceState({}, '', '/')
  server.use(
    http.get('http://localhost/api/v1/auth/me', () => HttpResponse.json(adminUser)),
    http.get('http://localhost/api/v1/auth/csrf', () =>
      HttpResponse.json({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'csrf-test-token' }),
    ),
    http.get('http://localhost/api/v1/glasses', () => HttpResponse.json({ content: mockGlasses })),
  )
})

it('lista vidros com contadores, busca em tempo real e filtros de espessura e acabamento', async () => {
  window.history.replaceState({}, '', '/glasses')
  const user = userEvent.setup()
  render(<App />)

  // Verifica carregamento dos itens
  expect(await screen.findByText('Vidro Temperado Incolor')).toBeInTheDocument()
  expect(screen.getByText('Vidro Laminado Fumê')).toBeInTheDocument()
  expect(screen.getByText('Vidro Comum Verde')).toBeInTheDocument()

  // Verifica contadores
  expect(screen.getByText(/Total:/)).toHaveTextContent('Total: 3 (2 ativos, 1 inativos)')

  // Testa busca
  const searchInput = screen.getByPlaceholderText('Buscar por nome, acabamento ou cor...')
  await user.type(searchInput, 'Fumê')
  expect(screen.getByText('Vidro Laminado Fumê')).toBeInTheDocument()
  expect(screen.queryByText('Vidro Temperado Incolor')).not.toBeInTheDocument()

  // Limpa busca
  await user.clear(searchInput)

  // Testa filtro por espessura
  const thicknessSelect = screen.getByLabelText('Filtrar por espessura')
  await user.selectOptions(thicknessSelect, '8')
  expect(screen.getByText('Vidro Temperado Incolor')).toBeInTheDocument()
  expect(screen.queryByText('Vidro Laminado Fumê')).not.toBeInTheDocument()

  // Reseta espessura e filtra por status
  await user.selectOptions(thicknessSelect, 'ALL')
  const statusSelect = screen.getByLabelText('Filtrar por status')
  await user.selectOptions(statusSelect, 'INACTIVE')
  expect(screen.getByText('Vidro Comum Verde')).toBeInTheDocument()
  expect(screen.queryByText('Vidro Temperado Incolor')).not.toBeInTheDocument()
})

it('cadastra novo vidro com seletores rápidos e envia payload correto', async () => {
  let createdBody: Record<string, unknown> | undefined
  server.use(
    http.post('http://localhost/api/v1/glasses', async ({ request }) => {
      expect(request.headers.get('X-XSRF-TOKEN')).toBe('csrf-test-token')
      createdBody = (await request.json()) as Record<string, unknown>
      return HttpResponse.json(
        {
          id: 'a4444444-4444-4444-4444-444444444444',
          name: 'Vidro Aramado',
          thicknessMm: 6,
          finish: 'Lapidado',
          color: 'Incolor',
          pricePerSquareMeter: 195.0,
          active: true,
          createdAt: '2026-09-14T10:00:00Z',
          updatedAt: '2026-09-14T10:00:00Z',
        },
        { status: 201 },
      )
    }),
  )

  window.history.replaceState({}, '', '/glasses/new')
  const user = userEvent.setup()
  render(<App />)

  await user.type(await screen.findByPlaceholderText('Ex: Vidro Temperado, Vidro Laminado, etc.'), 'Vidro Aramado')
  await user.click(screen.getByRole('button', { name: '6 mm' }))
  await user.click(screen.getByRole('button', { name: 'Cadastrar vidro' }))

  expect(await screen.findByText('Vidro cadastrado com sucesso no catálogo!')).toBeInTheDocument()
  expect(createdBody).toEqual({
    name: 'Vidro Aramado',
    thicknessMm: 6,
    finish: 'Lapidado',
    color: 'Incolor',
    pricePerSquareMeter: 150,
  })
})

it('edita vidro e exibe mensagem de duplicidade em caso de conflito', async () => {
  server.use(
    http.get('http://localhost/api/v1/glasses/a1111111-1111-1111-1111-111111111111', () =>
      HttpResponse.json(mockGlasses[0]),
    ),
    http.patch('http://localhost/api/v1/glasses/a1111111-1111-1111-1111-111111111111', () =>
      HttpResponse.json(
        {
          status: 409,
          title: 'Conflict',
          detail: 'Já existe um outro vidro cadastrado',
          code: 'DUPLICATE_GLASS',
          instance: '/api/v1/glasses/a1111111-1111-1111-1111-111111111111',
          timestamp: '2026-09-14T00:00:00Z',
        },
        { status: 409 },
      ),
    ),
  )

  window.history.replaceState({}, '', '/glasses/a1111111-1111-1111-1111-111111111111/edit')
  const user = userEvent.setup()
  render(<App />)

  expect(await screen.findByDisplayValue('Vidro Temperado Incolor')).toBeInTheDocument()

  await user.click(screen.getByRole('button', { name: 'Salvar alterações' }))

  expect(
    await screen.findByText('Já existe um outro vidro cadastrado com este mesmo nome, espessura, acabamento e cor.'),
  ).toBeInTheDocument()
})
