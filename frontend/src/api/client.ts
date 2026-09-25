import { ApiError, type FieldError } from './ApiError'

const BASE = import.meta.env.VITE_API_BASE_URL ?? '/api'

interface ProblemBody {
  status?: number
  code?: string
  detail?: string
  title?: string
  errors?: FieldError[]
}

async function parseProblem(response: Response): Promise<ApiError> {
  let body: ProblemBody = {}
  try {
    body = (await response.json()) as ProblemBody
  } catch {
    /* empty */
  }
  const code = body.code ?? 'INTERNAL_ERROR'
  const detail =
    body.detail ??
    body.title ??
    'Something went wrong on our side. Please try again.'
  return new ApiError(response.status, code, detail, body.errors ?? [])
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers)
  if (options.body != null && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json')
  }

  let response: Response
  try {
    response = await fetch(`${BASE}${path}`, { ...options, headers })
  } catch {
    throw ApiError.network()
  }

  if (response.status === 204) {
    return undefined as T
  }

  if (!response.ok) {
    throw await parseProblem(response)
  }

  return (await response.json()) as T
}
