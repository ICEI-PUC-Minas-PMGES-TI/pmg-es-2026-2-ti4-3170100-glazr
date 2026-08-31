import { render, screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { expect, it } from 'vitest'
import { server } from '../test/server'
import { App } from './App'

it('remove o token do fragmento antes de inspecionar o convite', async () => {
  let submittedToken: string | undefined
  window.history.replaceState({}, '', '/activate#token=convite-secreto')
  server.use(
    http.get('http://localhost/api/v1/auth/me', () => HttpResponse.json({ code: 'AUTHENTICATION_REQUIRED' }, { status: 401 })),
    http.get('http://localhost/api/v1/auth/csrf', () => HttpResponse.json({ headerName: 'X-XSRF-TOKEN', token: 'csrf' })),
    http.post('http://localhost/api/v1/invitations/inspect', async ({ request }) => {
      submittedToken = ((await request.json()) as { token: string }).token
      return HttpResponse.json({ displayName: 'P***', email: 'p***@example.com', expiresAt: '2026-09-02T12:00:00Z', availableMethods: ['PASSWORD'] })
    }),
  )

  render(<App />)

  expect(window.location.hash).toBe('')
  expect(await screen.findByText(/p\*\*\*@example.com/)).toBeInTheDocument()
  await waitFor(() => expect(submittedToken).toBe('convite-secreto'))
})
