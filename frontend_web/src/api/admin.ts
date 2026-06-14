import { get, post, put, del } from './client'
import type { Page, Result } from './client'

export interface AdminVO {
  id: string
  username: string
  email: string
  is_super: boolean
  created_at: string
  updated_at: string
}

export interface AdminCreateRequest {
  username: string
  password: string
  email: string
}

export interface AdminUpdateRequest {
  username?: string
  password?: string
  email?: string
}

export function listAdmins(params: { page?: number; size?: number; keyword?: string }) {
  const query = new URLSearchParams()
  if (params.page) query.set('page', String(params.page))
  if (params.size) query.set('size', String(params.size))
  if (params.keyword) query.set('keyword', params.keyword)
  return get<Result<Page<AdminVO>>>(`/admin/admins/?${query.toString()}`)
}

export function getAdmin(id: string) {
  return get<Result<AdminVO>>(`/admin/admins/${id}`)
}

export function createAdmin(body: AdminCreateRequest) {
  return post<Result<AdminVO>>('/admin/admins/', body)
}

export function updateAdmin(id: string, body: AdminUpdateRequest) {
  return put<Result<AdminVO>>(`/admin/admins/${id}`, body)
}

export function deleteAdmin(id: string) {
  return del<Result<null>>(`/admin/admins/${id}`)
}
