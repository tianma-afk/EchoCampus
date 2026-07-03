import axios from 'axios'
import { API_BASE } from '../config'

const FEEDBACK_API = `${API_BASE}/api/v1/user/feedbacks`

export interface FeedbackRecord {
  id: string
  landmarkId: string
  landmarkName: string
  feedbackType: string
  content: string
  status: string
  resolveNote: string | null
  resolveTime: string | null
  createdAt: string
  uploadUrl: string | null
  correctLandmarkName: string | null
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export async function getMyFeedbacks(page = 1, size = 10): Promise<{ code: string; message: string; data: PageResult<FeedbackRecord> }> {
  const res = await axios.get(`${FEEDBACK_API}/`, { params: { page, size } })
  return res.data
}

export async function getFeedbackDetail(id: string): Promise<{ code: string; message: string; data: FeedbackRecord }> {
  const res = await axios.get(`${FEEDBACK_API}/${id}`)
  return res.data
}

export const FEEDBACK_TYPE_MAP: Record<string, string> = {
  INFO_ERROR: '信息错误',
  CONTENT_ILLEGAL: '违禁内容',
  INFO_CHANGE: '信息变更',
  OTHER: '其他',
  ADD_LANDMARK: '新增地标',
}

export const FEEDBACK_STATUS_MAP: Record<string, { label: string; color: string; bg: string }> = {
  PENDING: { label: '待处理', color: '#d97706', bg: '#fef3c7' },
  RESOLVED: { label: '已解决', color: '#059669', bg: '#ecfdf5' },
  REJECTED: { label: '已驳回', color: '#dc2626', bg: '#fef2f2' },
}
