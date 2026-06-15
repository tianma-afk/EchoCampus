import axios from 'axios'

const API_BASE = 'http://localhost:8080/api/v1/user/favorites'

export interface FavoriteRecord {
  id: string
  landmarkId: string
  landmarkName: string
  coverImageUrl: string
  landmarkRating: number
  category: string
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export async function toggleFavorite(landmarkId: string): Promise<{ code: string; message: string; data: boolean }> {
  const res = await axios.post(API_BASE, { landmarkId })
  return res.data
}

export async function getFavorites(page = 1, size = 10): Promise<{ code: string; message: string; data: PageResult<FavoriteRecord> }> {
  const res = await axios.get(API_BASE, { params: { page, size } })
  return res.data
}

export async function isFavorited(landmarkId: string): Promise<{ code: string; message: string; data: boolean }> {
  const res = await axios.get(`${API_BASE}/${landmarkId}`)
  return res.data
}
