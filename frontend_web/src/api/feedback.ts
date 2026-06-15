import { get, put, type Page, type Result } from './client'

export interface FeedbackAdminVO {
  id: string
  landmarkId: string
  landmarkName: string
  userId: string
  feedbackType: string
  content: string
  status: string
  adminId: string | null
  adminName: string | null
  resolveTime: string | null
  resolveNote: string | null
  createdAt: string
  uploadUrl: string | null
  correctLandmarkName: string | null
}

export interface FeedbackResolveRequest {
  status: string
  resolveNote?: string
}

export const FEEDBACK_TYPE_MAP: Record<string, string> = {
  INFO_ERROR: '信息错误',
  CONTENT_ILLEGAL: '违禁内容',
  INFO_CHANGE: '信息变更',
  OTHER: '其他',
}

export const FEEDBACK_STATUS_MAP: Record<string, string> = {
  PENDING: '待处理',
  RESOLVED: '已解决',
  REJECTED: '已驳回',
}

export function getFeedbackList(
  page: number,
  pageSize: number,
  status?: string,
  feedbackType?: string,
): Promise<Result<Page<FeedbackAdminVO>>> {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('pageSize', String(pageSize))
  if (status) params.set('status', status)
  if (feedbackType) params.set('feedbackType', feedbackType)
  return get(`/admin/feedbacks/?${params.toString()}`)
}

export function getFeedbackDetail(id: string): Promise<Result<FeedbackAdminVO>> {
  return get(`/admin/feedbacks/${id}`)
}

export function resolveFeedback(
  id: string,
  data: FeedbackResolveRequest,
): Promise<Result<null>> {
  return put(`/admin/feedbacks/${id}/resolve`, data)
}
