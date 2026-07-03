import axios from 'axios'
import { API_BASE } from '../config'

const RECOGNITION_API = `${API_BASE}/api/v1/user/recognitions`

export interface RecognitionRecord {
  id: string
  imageUrl: string
  landmarkId: string | null
  landmarkName: string | null
  coverImageUrl: string | null
  similarity: number | null
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export async function getRecognitions(page = 1, size = 10): Promise<{ code: string; message: string; data: PageResult<RecognitionRecord> }> {
  const res = await axios.get(RECOGNITION_API, { params: { page, size } })
  return res.data
}

export async function batchDeleteRecognitions(ids: string[]): Promise<{ code: string; message: string }> {
  const res = await axios.delete(RECOGNITION_API, { data: { ids } })
  return res.data
}
