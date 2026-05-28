import { get, post, put, del } from './client'
import type { Result, Page } from './client'

export interface CampusVO {
  id: string
  name: string
  universityId: string
}

export function searchCampuses(
  keyword: string,
  universityId?: string,
): Promise<Result<CampusVO[]>> {
  const params = new URLSearchParams()
  if (keyword) params.set('keyword', keyword)
  if (universityId) params.set('universityId', universityId)
  const qs = params.toString()
  return get<Result<CampusVO[]>>(`/admin/campuses/search${qs ? '?' + qs : ''}`)
}

export function getCampusList(
  page: number,
  pageSize: number,
  universityId?: string,
): Promise<Result<Page<CampusVO>>> {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('pageSize', String(pageSize))
  if (universityId) params.set('universityId', universityId)
  return get<Result<Page<CampusVO>>>(`/admin/campuses/?${params.toString()}`)
}

export function getCampusById(id: string): Promise<Result<CampusVO>> {
  return get<Result<CampusVO>>(`/admin/campuses/${id}`)
}

export function createCampus(name: string, universityId: string): Promise<Result<string>> {
  return post<Result<string>>('/admin/campuses/', { name, universityId })
}

export function updateCampus(id: string, name: string, universityId?: string): Promise<Result<void>> {
  return put<Result<void>>(`/admin/campuses/${id}`, { name, universityId })
}

export function deleteCampus(id: string): Promise<Result<void>> {
  return del<Result<void>>(`/admin/campuses/${id}`)
}
