import { get, post, put, del } from './client'
import type { Result } from './client'

export interface LandmarkImageVO {
  id: string
  url: string
  cover: boolean
  curated: boolean
  fileExt: string
  createdAt: string
}

export interface ImagePageData {
  records: LandmarkImageVO[]
  total: number
  hasMore: boolean
}

export interface ImagePresignResponse {
  key: string
  presignedUrl: string
}

export interface BatchDeleteResponse {
  deletedCount: number
  affectedCover: boolean
  affectedCuratedCount: number
}

export function listImages(
  landmarkId: string,
  page = 1,
  pageSize = 20,
): Promise<Result<ImagePageData>> {
  return get<Result<ImagePageData>>(
    `/admin/landmarks/${landmarkId}/images?page=${page}&pageSize=${pageSize}`,
  )
}

export function presignUpload(
  landmarkId: string,
  filename: string,
): Promise<Result<ImagePresignResponse>> {
  return post<Result<ImagePresignResponse>>(`/admin/landmarks/${landmarkId}/images/presign`, {
    filename,
  })
}

export function confirmUpload(landmarkId: string, key: string): Promise<Result<string>> {
  return post<Result<string>>(`/admin/landmarks/${landmarkId}/images/confirm`, { key })
}

export function setCover(landmarkId: string, imageId: string): Promise<Result<void>> {
  return put<Result<void>>(`/admin/landmarks/${landmarkId}/cover`, { imageId })
}

export function setCuratedImages(
  landmarkId: string,
  imageIds: string[],
): Promise<Result<void>> {
  return put<Result<void>>(`/admin/landmarks/${landmarkId}/images/curated`, { imageIds })
}

export function deleteImage(landmarkId: string, imageId: string): Promise<Result<void>> {
  return del<Result<void>>(`/admin/landmarks/${landmarkId}/images/${imageId}`)
}

export function deleteImagesBatch(
  landmarkId: string,
  imageIds: string[],
): Promise<Result<BatchDeleteResponse>> {
  return delWithBody<Result<BatchDeleteResponse>>(
    `/admin/landmarks/${landmarkId}/images/batch`,
    { imageIds },
  )
}

async function delWithBody<T>(url: string, body: unknown): Promise<T> {
  const BASE_URL = '/api/v1'
  const response = await fetch(`${BASE_URL}${url}`, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }
  return response.json()
}
