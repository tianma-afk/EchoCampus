import axios from 'axios'
import { API_BASE } from '../config'

const LANDMARK_API = `${API_BASE}/api/v1/landmarks`
const USER_API = `${API_BASE}/api/v1/user`

export async function doCheckin(
  landmarkId: string,
  latitude: number,
  longitude: number
): Promise<{ code: string; message: string }> {
  const res = await axios.post(`${LANDMARK_API}/${landmarkId}/checkin`, { latitude, longitude })
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
  const res = await axios.get(`${USER_API}/checkins`, { params: { page, size } })
  return res.data
}

export async function batchDeleteCheckins(ids: string[]): Promise<{ code: string; message: string }> {
  const res = await axios.delete(`${USER_API}/checkins`, { data: { ids } })
  return res.data
}
