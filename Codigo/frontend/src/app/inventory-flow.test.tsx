import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, expect, it } from 'vitest'

import { App } from './App'
import { server } from '../test/server'

const stockUser = {
  id: '1f5fb5d7-37ba-4e15-9d72-186da2db78d0',
  email: 'estoque@glazr.local',
  displayName: 'Operador de Estoque',
  active: true,
  profiles: ['ADMINISTRADOR'],
  permissions: ['GLASS_READ', 'GLASS_UPDATE'],
}

const lowStock = {
  glassId: 'a1111111-1111-1111-1111-111111111111',
  glassName: 'Vidro Temperado Incolor',
  thicknessMm: 8,
  finish: 'Lapidado',
  color: 'Incolor',
  glassActive: true,
  availableAreaM2: 3.5,
  minimumAreaM2: 4,
  status: 'LOW',
}

const unconfiguredStock = {
  glassId: 'a2222222-2222-2222-2222-222222222222',
  glassName: 'Vidro Laminado Fumê',
  thicknessMm: 10,
  finish: 'Bisotê',
  color: 'Fumê',
  glassActive: true,
  availableAreaM2: 6,
  minimumAreaM2: null,
  status: 'UNCONFIGURED',
}

const availableSheet = {
  id: 'b1111111-1111-1111-1111-111111111111',
  glassId: lowStock.glassId,
  originalWidthMm: 2000,
  originalHeightMm: 3000,
  originalAreaM2: 6,
  remainingAreaM2: 3.5,
  status: 'AVAILABLE',
  createdAt: '2026-09-14T10:00:00Z',
  updatedAt: '2026-09-14T10:00:00Z',
}

beforeEach(() => {
  window.history.replaceState({}, '', '/')
  server.use(
    http.get('http://localhost/api/v1/auth/me', () => HttpResponse.json(stockUser)),
    http.get('http://localhost/api/v1/auth/csrf', () =>
      HttpResponse.json({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'csrf-test-token' }),
    ),
  )
})

it('lists persistent low-stock alerts and filters inventory summaries', async () => {
  server.use(
    http.get('http://localhost/api/v1/inventory/glasses', () =>
      HttpResponse.json({ content: [lowStock, unconfiguredStock] }),
    ),
  )
  window.history.replaceState({}, '', '/inventory')
  const user = userEvent.setup()
  render(<App />)

  expect(await screen.findByText('Vidro Temperado Incolor')).toBeInTheDocument()
  expect(screen.getAllByText('Estoque baixo').length).toBeGreaterThan(0)
  expect(screen.getAllByText('Mínimo não configurado').length).toBeGreaterThan(0)

  await user.selectOptions(screen.getByLabelText('Filtrar situação do estoque'), 'LOW')

  expect(screen.getByText('Vidro Temperado Incolor')).toBeInTheDocument()
  expect(screen.queryByText('Vidro Laminado Fumê')).not.toBeInTheDocument()
})

it('registers a sheet receipt with the authenticated CSRF flow', async () => {
  let receivedPayload: Record<string, unknown> | undefined
  server.use(
    http.get(`http://localhost/api/v1/inventory/glasses/${lowStock.glassId}`, () => HttpResponse.json(lowStock)),
    http.get(`http://localhost/api/v1/inventory/glasses/${lowStock.glassId}/sheets`, () =>
      HttpResponse.json({ content: [availableSheet] }),
    ),
    http.get(`http://localhost/api/v1/inventory/glasses/${lowStock.glassId}/movements`, () =>
      HttpResponse.json({ content: [] }),
    ),
    http.post(`http://localhost/api/v1/inventory/glasses/${lowStock.glassId}/receipts`, async ({ request }) => {
      expect(request.headers.get('X-XSRF-TOKEN')).toBe('csrf-test-token')
      receivedPayload = (await request.json()) as Record<string, unknown>
      return HttpResponse.json([availableSheet], { status: 201 })
    }),
  )
  window.history.replaceState({}, '', `/inventory/glasses/${lowStock.glassId}`)
  const user = userEvent.setup()
  render(<App />)

  expect(await screen.findByText('Vidro Temperado Incolor')).toBeInTheDocument()
  await user.clear(screen.getByLabelText('Quantidade de chapas'))
  await user.type(screen.getByLabelText('Quantidade de chapas'), '2')
  await user.type(screen.getByLabelText('Observação do recebimento'), 'Compra semanal')
  await user.click(screen.getByRole('button', { name: 'Registrar recebimento' }))

  expect(await screen.findByText('Chapas recebidas e adicionadas ao estoque.')).toBeInTheDocument()
  expect(receivedPayload).toEqual({
    widthMm: 2000,
    heightMm: 3000,
    quantity: 2,
    notes: 'Compra semanal',
  })
})

it('registers a manual cut and displays the calculated yield', async () => {
  let cutPayload: Record<string, unknown> | undefined
  server.use(
    http.get(`http://localhost/api/v1/inventory/glasses/${lowStock.glassId}`, () => HttpResponse.json(lowStock)),
    http.get(`http://localhost/api/v1/inventory/glasses/${lowStock.glassId}/sheets`, () =>
      HttpResponse.json({ content: [availableSheet] }),
    ),
    http.get(`http://localhost/api/v1/inventory/glasses/${lowStock.glassId}/movements`, () =>
      HttpResponse.json({ content: [] }),
    ),
    http.post(`http://localhost/api/v1/inventory/sheets/${availableSheet.id}/cuts`, async ({ request }) => {
      cutPayload = (await request.json()) as Record<string, unknown>
      return HttpResponse.json({
        sheet: { ...availableSheet, remainingAreaM2: 2.5 },
        movement: {
          id: 'c1111111-1111-1111-1111-111111111111',
          glassSheetId: availableSheet.id,
          movementType: 'MANUAL_CUT',
          previousRemainingAreaM2: 3.5,
          resultingRemainingAreaM2: 2.5,
          pieceWidthMm: 1000,
          pieceHeightMm: 1000,
          pieceAreaM2: 1,
          lossAreaM2: 0,
          notes: null,
          performedByUserId: stockUser.id,
          createdAt: '2026-09-14T11:00:00Z',
        },
        yieldPercent: 100,
      })
    }),
  )
  window.history.replaceState({}, '', `/inventory/glasses/${lowStock.glassId}`)
  const user = userEvent.setup()
  render(<App />)

  await screen.findByText('Vidro Temperado Incolor')
  await user.type(screen.getByLabelText('Largura da peça'), '1000')
  await user.type(screen.getByLabelText('Altura da peça'), '1000')
  await user.type(screen.getByLabelText('Sobra útil em metros quadrados'), '2.5')
  await user.click(screen.getByRole('button', { name: 'Registrar corte' }))

  expect(await screen.findByText('Corte registrado: aproveitamento de 100,00%.')).toBeInTheDocument()
  expect(cutPayload).toEqual({
    pieceWidthMm: 1000,
    pieceHeightMm: 1000,
    usableRemainderAreaM2: 2.5,
    notes: null,
  })
})

it('shows the low-stock summary on the dashboard', async () => {
  server.use(
    http.get('http://localhost/api/v1/inventory/glasses/alerts', () => HttpResponse.json([lowStock])),
  )
  window.history.replaceState({}, '', '/dashboard')
  render(<App />)

  expect(await screen.findByText(/1 tipo\(s\) de vidro/)).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'Ver estoque' })).toHaveAttribute('href', '/inventory')
})
