import { post } from './client'
import type { Result } from './client'

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
  floorList?: FloorCreateDTO[]
}

export function createLandmark(data: LandmarkCreateRequest): Promise<Result<string>> {
  return post<Result<string>>('/admin/landmarks/', data)
}
