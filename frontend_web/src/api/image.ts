import { get, post, put, del } from './client'
import type { Result } from './client'

export interface LandmarkImageVO {
  id: string
  url: string
  isCover: boolean
  isCurated: boolean
  fileExt: string
  createdAt: string
}

export interface ImagePresignResponse {
  key: string
  presignedUrl: string
}

export function listImages(landmarkId: string): Promise<Result<LandmarkImageVO[]>> {
  return get<Result<LandmarkImageVO[]>>(`/admin/landmarks/${landmarkId}/images`)
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
