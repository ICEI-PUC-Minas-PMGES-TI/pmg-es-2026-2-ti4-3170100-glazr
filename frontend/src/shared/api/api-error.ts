export interface ProblemDetail {
  status?: number
  title?: string
  detail?: string
  code?: string
  instance?: string
  timestamp?: string
  fieldErrors?: Array<{ field: string; message: string }>
}

export class ApiError extends Error {
  readonly status: number
  readonly code?: string
  readonly problem?: ProblemDetail

  constructor(status: number, message: string, problem?: ProblemDetail) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = problem?.code
    this.problem = problem
  }
}
