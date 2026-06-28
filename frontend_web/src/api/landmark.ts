import { get, post, put, del } from './client'
import type { Result, Page } from './client'

export interface FloorCreateDTO {
  floorNumber: number
  floorName: string
  tags?: string[]
}

export interface LandmarkCreateRequest {
  name: string
  rating?: number
  checkInCount?: number
  openTime?: string
  categoryId: string
  tags?: string[]
  buildYear?: string
  openTimeDetail?: string
  floors?: string
  location?: string
  description?: string
  campusId: string
  totalFloors?: number
  recommendRate?: number
  latitude?: number
  longitude?: number
  floorList?: FloorCreateDTO[]
}

export interface LandmarkAdminVO {
  id: string
  name: string
  categoryName: string
  categoryId: string
  universityName: string | null
  campusName: string
  campusId: string
  coverImageUrl: string | null
  rating: number | null
  checkInCount: number | null
  recommendRate: number | null
  favoriteCount: number | null
  buildYear: string | null
  openTime: string | null
}

export function getLandmarkList(
  page: number,
  pageSize: number,
  categoryId?: string,
  campusId?: string,
  keyword?: string,
): Promise<Result<Page<LandmarkAdminVO>>> {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('pageSize', String(pageSize))
  if (categoryId) params.set('categoryId', categoryId)
  if (campusId) params.set('campusId', campusId)
  if (keyword) params.set('keyword', keyword)
  return get<Result<Page<LandmarkAdminVO>>>(`/admin/landmarks/?${params.toString()}`)
}

export interface FloorVO {
  id: string
  floorNumber: number
  floorName: string
  tags?: string[]
}

export interface LandmarkDetailVO {
  id: string
  name: string
  rating: number | null
  checkins: number | null
  openTime: string | null
  category: string | null
  categoryId: string
  tags: string[] | null
  imgs: string[] | null
  coverImageUrl: string | null
  buildYear: string | null
  openTimeDetail: string | null
  floors: string | null
  location: string | null
  description: string | null
  campusName: string | null
  campusId: string
  universityName: string | null
  universityId: string | null
  totalFloors: number | null
  recommendRate: number | null
  latitude: number | null
  longitude: number | null
  floorList: FloorVO[] | null
}

export function getLandmarkDetail(id: string): Promise<Result<LandmarkDetailVO>> {
  return get<Result<LandmarkDetailVO>>(`/admin/landmarks/${id}`)
}

export function createLandmark(data: LandmarkCreateRequest): Promise<Result<string>> {
  return post<Result<string>>('/admin/landmarks/', data)
}

export function updateLandmark(id: string, data: Partial<LandmarkCreateRequest>): Promise<Result<void>> {
  return put<Result<void>>(`/admin/landmarks/${id}`, data)
}

export function deleteLandmark(id: string): Promise<Result<void>> {
  return del<Result<void>>(`/admin/landmarks/${id}`)
}
