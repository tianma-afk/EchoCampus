import axios from 'axios'

const API_BASE = 'http://localhost:8080/api/v1/user/comments'

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
  const res = await axios.get(API_BASE, { params: { landmarkId } })
  return res.data
}

export async function submitComment(landmarkId: string, content: string, parentId?: string): Promise<{ code: string; message: string; data: null }> {
  const res = await axios.post(API_BASE, { landmarkId, content, parentId: parentId || null })
  return res.data
}

export async function likeComment(commentId: string): Promise<{ code: string; message: string; data: null }> {
  const res = await axios.post(`${API_BASE}/${commentId}/like`)
  return res.data
}

export async function unlikeComment(commentId: string): Promise<{ code: string; message: string; data: null }> {
  const res = await axios.delete(`${API_BASE}/${commentId}/like`)
  return res.data
}
