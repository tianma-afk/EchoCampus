import axios from 'axios'
import { API_BASE } from '../config'

const RATING_API = `${API_BASE}/api/v1/user/ratings`

export interface RatingRecord {
  id: string
  landmarkId: string
  landmarkName: string
  coverImageUrl: string
  landmarkRating: number
  category: string
  rating: number
  createdAt: string
  updatedAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export async function submitRating(landmarkId: string, rating: number): Promise<{ code: string; message: string }> {
  const res = await axios.post(RATING_API, { landmarkId, rating })
  return res.data
}

export async function getUserRating(landmarkId: string): Promise<{ code: string; message: string; data: RatingRecord | null }> {
  const res = await axios.get(`${RATING_API}/${landmarkId}`)
  return res.data
}

export async function getUserRatings(page = 1, size = 10): Promise<{ code: string; message: string; data: PageResult<RatingRecord> }> {
  const res = await axios.get(RATING_API, { params: { page, size } })
  return res.data
}

export async function batchDeleteRatings(ids: string[]): Promise<{ code: string; message: string }> {
  const res = await axios.delete(RATING_API, { data: { ids } })
  return res.data
}
