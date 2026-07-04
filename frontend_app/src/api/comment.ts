import axios from 'axios'
import { API_BASE } from '../config'

const API_URL = `${API_BASE}/api/v1/user/comments`

export interface CommentItem {
  id: string
  landmarkId: string
  userId: string
  nickname: string
  parentId: string | null
  content: string
  likeCount: number
  isLiked: boolean
  createdAt: string
  replies: CommentItem[]
}

export async function getComments(landmarkId: string): Promise<{ code: string; message: string; data: CommentItem[] }> {
  const res = await axios.get(API_URL, { params: { landmarkId } })
  return res.data
}

export async function submitComment(landmarkId: string, content: string, parentId?: string): Promise<{ code: string; message: string; data: null }> {
  const res = await axios.post(API_URL, { landmarkId, content, parentId: parentId || null })
  return res.data
}

export async function likeComment(commentId: string): Promise<{ code: string; message: string; data: null }> {
  const res = await axios.post(`${API_URL}/${commentId}/like`)
  return res.data
}

export async function unlikeComment(commentId: string): Promise<{ code: string; message: string; data: null }> {
  const res = await axios.delete(`${API_URL}/${commentId}/like`)
  return res.data
}
