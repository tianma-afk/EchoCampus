import axios from 'axios'

const API_BASE = 'http://localhost:8080/api/v1/user/recognitions'

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
  const res = await axios.get(API_BASE, { params: { page, size } })
  return res.data
}
