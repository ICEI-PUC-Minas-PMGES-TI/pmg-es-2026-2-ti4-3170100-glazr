import { afterEach, describe, expect, it, vi } from 'vitest'

import { requestJson } from './http-client'

describe('requestJson', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('transforma Problem Details em um erro de API seguro', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            status: 401,
            title: 'Unauthorized',
            detail: 'Credenciais inválidas',
            code: 'AUTHENTICATION_FAILED',
            instance: '/api/v1/auth/login',
            timestamp: '2026-08-31T00:00:00Z',
          }),
          {
            status: 401,
            headers: { 'Content-Type': 'application/problem+json' },
          },
        ),
      ),
    )

    await expect(requestJson('/api/v1/auth/login')).rejects.toMatchObject({
      name: 'ApiError',
      status: 401,
      code: 'AUTHENTICATION_FAILED',
      message: 'Credenciais inválidas',
    })
  })
})
