import { requestJson } from './http-client'

interface CsrfResponse {
  headerName: string
  token: string
}

export async function csrfRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const csrf = await requestJson<CsrfResponse>('/api/v1/auth/csrf')
  return requestJson<T>(path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init.headers, [csrf.headerName]: csrf.token },
  })
}
