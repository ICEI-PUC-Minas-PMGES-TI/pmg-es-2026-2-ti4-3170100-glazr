import { render, screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, expect, it } from 'vitest'

import { App } from './App'
import { server } from '../test/server'

const analyticsUser = {
  id: '1f5fb5d7-37ba-4e15-9d72-186da2db78d0',
  email: 'gestor@glazr.local',
  displayName: 'Gestor',
  active: true,
  profiles: ['ADMINISTRADOR'],
  permissions: ['GLASS_READ', 'KIT_READ'],
}

const glass = {
  thicknessMm: 8,
  finish: 'Lapidado',
  color: 'Incolor',
  active: true,
  createdAt: '2026-09-14T10:00:00Z',
  updatedAt: '2026-09-14T10:00:00Z',
}

beforeEach(() => {
  window.history.replaceState({}, '', '/')
  server.use(
    http.get('http://localhost/api/v1/auth/me', () => HttpResponse.json(analyticsUser)),
    http.get('http://localhost/api/v1/inventory/glasses', () =>
      HttpResponse.json({
        content: [
          { glassId: 'a1', glassName: 'Temperado Incolor', thicknessMm: 8, finish: 'Lapidado', color: 'Incolor', glassActive: true, availableAreaM2: 3.5, minimumAreaM2: 4, status: 'LOW' },
          { glassId: 'a2', glassName: 'Laminado Fumê', thicknessMm: 10, finish: 'Bisotê', color: 'Fumê', glassActive: true, availableAreaM2: 12, minimumAreaM2: 5, status: 'OK' },
        ],
      }),
    ),
    http.get('http://localhost/api/v1/glasses', () =>
      HttpResponse.json({
        content: [
          { ...glass, id: 'a1', name: 'Temperado Incolor', pricePerSquareMeter: 100 },
          { ...glass, id: 'a2', name: 'Temperado Verde', pricePerSquareMeter: 200 },
          { ...glass, id: 'a3', name: 'Laminado Fumê', finish: 'Bisotê', thicknessMm: 10, pricePerSquareMeter: 300 },
          { ...glass, id: 'a4', name: 'Inativo', pricePerSquareMeter: 999, active: false },
        ],
      }),
    ),
    http.get('http://localhost/api/v1/kits', () => HttpResponse.json({ content: [] })),
  )
})

it('summarizes inventory and catalog data on the analytics page', async () => {
  window.history.replaceState({}, '', '/analytics')
  render(<App />)

  expect(await screen.findByRole('heading', { name: 'Indicadores' })).toBeInTheDocument()
  const metrics = await screen.findByLabelText('Resumo dos indicadores')
  expect(within(metrics).getByText('15,50 m²')).toBeInTheDocument()
  expect(within(metrics).getByText('Kits cadastrados')).toBeInTheDocument()

  const priceTable = screen.getByRole('heading', { name: 'Preço médio por espessura' }).closest('figure')
  expect(priceTable).not.toBeNull()
  expect(within(priceTable as HTMLElement).getByText('R$ 150,00/m²')).toBeInTheDocument()
  expect(within(priceTable as HTMLElement).getByText('R$ 300,00/m²')).toBeInTheDocument()
})
