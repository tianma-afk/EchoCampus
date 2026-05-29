import { get, post, put, del } from './client'
import type { Result, Page } from './client'

export interface UniversityVO {
  id: string
  name: string
}

export async function searchUniversities(keyword: string): Promise<Result<UniversityVO[]>> {
  const params = new URLSearchParams()
  if (keyword) params.set('keyword', keyword)
  const qs = params.toString()
  const res = await get<Result<{ records: UniversityVO[] }>>(
    `/admin/universities/search${qs ? '?' + qs : ''}`,
  )
  return {
    code: res.code,
    data: res.data.records,
    message: res.message,
  }
}

export function getUniversityList(page: number, pageSize: number): Promise<Result<Page<UniversityVO>>> {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('pageSize', String(pageSize))
  return get<Result<Page<UniversityVO>>>(`/admin/universities/?${params.toString()}`)
}

export function getUniversityById(id: string): Promise<Result<UniversityVO>> {
  return get<Result<UniversityVO>>(`/admin/universities/${id}`)
}

export function createUniversity(name: string): Promise<Result<string>> {
  return post<Result<string>>('/admin/universities/', { name })
}

export function updateUniversity(id: string, name: string): Promise<Result<void>> {
  return put<Result<void>>(`/admin/universities/${id}`, { name })
}

export function deleteUniversity(id: string): Promise<Result<void>> {
  return del<Result<void>>(`/admin/universities/${id}`)
}
