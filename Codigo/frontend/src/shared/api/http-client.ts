import { ApiError, type ProblemDetail } from './api-error'

const NETWORK_ERROR_MESSAGE = 'Não foi possível conectar ao servidor. Tente novamente.'
const UNEXPECTED_ERROR_MESSAGE = 'Não foi possível processar a solicitação.'

export async function requestJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response

  try {
    response = await fetch(path, {
      ...init,
      credentials: 'include',
      headers: {
        Accept: 'application/json, application/problem+json',
        ...init.headers,
      },
    })
  } catch {
    throw new ApiError(0, NETWORK_ERROR_MESSAGE)
  }

  const contentType = response.headers.get('Content-Type') ?? ''
  const hasJson = contentType.includes('json')
  const body = hasJson ? ((await response.json()) as unknown) : undefined

  if (!response.ok) {
    const problem = body as ProblemDetail | undefined
    throw new ApiError(
      response.status,
      problem?.detail || UNEXPECTED_ERROR_MESSAGE,
      problem,
    )
  }

  return body as T
}
