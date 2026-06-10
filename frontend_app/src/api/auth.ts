import axios from 'axios'

const API_BASE = 'http://localhost:8080/api/v1/auth'

export interface SendCodeResult {
  expires_in: number
}

export interface LoginResult {
  access_token: string
  token_type: string
  expires_in: number
  nickname: string
}

export interface ApiResponse<T> {
  code: string
  message: string
  data: T
}

export async function sendCode(email: string): Promise<ApiResponse<SendCodeResult>> {
  const res = await axios.post(`${API_BASE}/send-code`, { email })
  return res.data
}

export async function register(params: {
  email: string
  code: string
  password: string
  nickname: string
}): Promise<ApiResponse<LoginResult>> {
  const res = await axios.post(`${API_BASE}/register`, params)
  return res.data
}

export async function login(params: {
  email: string
  password: string
}): Promise<ApiResponse<LoginResult>> {
  const res = await axios.post(`${API_BASE}/login`, params)
  return res.data
}
