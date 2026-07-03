import axios from 'axios'
import { API_BASE } from '../config'

const FAVORITE_API = `${API_BASE}/api/v1/user/favorites`

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
  const res = await axios.post(FAVORITE_API, { landmarkId })
  return res.data
}

export async function getFavorites(page = 1, size = 10): Promise<{ code: string; message: string; data: PageResult<FavoriteRecord> }> {
  const res = await axios.get(FAVORITE_API, { params: { page, size } })
  return res.data
}

export async function isFavorited(landmarkId: string): Promise<{ code: string; message: string; data: boolean }> {
  const res = await axios.get(`${FAVORITE_API}/${landmarkId}`)
  return res.data
}

export async function batchDeleteFavorites(ids: string[]): Promise<{ code: string; message: string }> {
  const res = await axios.delete(FAVORITE_API, { data: { ids } })
  return res.data
}
