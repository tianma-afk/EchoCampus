import axios from 'axios'

const API_BASE = 'http://localhost:8080/api/v1/landmarks'
const USER_API_BASE = 'http://localhost:8080/api/v1/user'

export async function doCheckin(
  landmarkId: string,
  latitude: number,
  longitude: number
): Promise<{ code: string; message: string }> {
  const res = await axios.post(`${API_BASE}/${landmarkId}/checkin`, { latitude, longitude })
  return res.data
}

export interface CheckinRecord {
  id: string
  landmarkId: string
  landmarkName: string
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export async function getCheckinHistory(page = 1, size = 10): Promise<{ code: string; message: string; data: PageResult<CheckinRecord> }> {
  const res = await axios.get(`${USER_API_BASE}/checkins`, { params: { page, size } })
  return res.data
}
