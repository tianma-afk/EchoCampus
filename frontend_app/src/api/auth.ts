import axios from 'axios'
import { API_BASE } from '../config'

const API_BASE_AUTH = `${API_BASE}/api/v1/auth`

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
  const res = await axios.post(`${API_BASE_AUTH}/send-code`, { email })
  return res.data
}

export async function register(params: {
  email: string
  code: string
  password: string
  nickname: string
}): Promise<ApiResponse<LoginResult>> {
  const res = await axios.post(`${API_BASE_AUTH}/register`, params)
  return res.data
}

export async function login(params: {
  email: string
  password: string
}): Promise<ApiResponse<LoginResult>> {
  const res = await axios.post(`${API_BASE_AUTH}/login`, params)
  return res.data
}

export interface UserProfile {
  id: string
  nickname: string
  email: string
  remainingNicknameChanges: number
}

export async function getProfile(token: string): Promise<ApiResponse<UserProfile>> {
  const res = await axios.get(`${API_BASE}/api/v1/user/profile`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  return res.data
}

export async function updateProfile(nickname: string): Promise<ApiResponse<UserProfile>> {
  const res = await axios.put(`${API_BASE}/api/v1/user/profile`, { nickname })
  return res.data
}
