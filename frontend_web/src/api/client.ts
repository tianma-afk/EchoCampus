export interface Result<T> {
  code: string
  data: T
  message: string
}

export interface Page<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

const BASE_URL = '/api/v1'
const TOKEN_KEY = 'echocampus_token'

interface RequestOptions {
  method?: string
  body?: unknown
}

async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body } = options
  const headers: Record<string, string> = {}

  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  if (body) {
    headers['Content-Type'] = 'application/json'
  }

  const response = await fetch(`${BASE_URL}${url}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  if (response.status === 401) {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem('echocampus_username')
    localStorage.removeItem('echocampus_role')
    localStorage.removeItem('echocampus_admin_id')
    window.location.href = '/login'
    throw new Error('Unauthorized')
  }

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

export function put<T>(url: string, body: unknown): Promise<T> {
  return request<T>(url, { method: 'PUT', body })
}

export function del<T>(url: string): Promise<T> {
  return request<T>(url, { method: 'DELETE' })
}
