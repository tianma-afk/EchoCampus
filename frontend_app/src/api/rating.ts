import axios from 'axios'

const API_BASE = 'http://localhost:8080/api/v1/user/ratings'

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
  const res = await axios.post(API_BASE, { landmarkId, rating })
  return res.data
}

export async function getUserRating(landmarkId: string): Promise<{ code: string; message: string; data: RatingRecord | null }> {
  const res = await axios.get(`${API_BASE}/${landmarkId}`)
  return res.data
}

export async function getUserRatings(page = 1, size = 10): Promise<{ code: string; message: string; data: PageResult<RatingRecord> }> {
  const res = await axios.get(API_BASE, { params: { page, size } })
  return res.data
}
