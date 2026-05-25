export interface Result<T> {
  code: number
  data: T
  message: string
}

const BASE_URL = '/api/v1'

interface RequestOptions {
  method?: string
  body?: unknown
}

async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body } = options
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }

  const response = await fetch(`${BASE_URL}${url}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }

  return response.json()
}

export function get<T>(url: string): Promise<T> {
  return request<T>(url)
}

export function post<T>(url: string, body: unknown): Promise<T> {
  return request<T>(url, { method: 'POST', body })
}
