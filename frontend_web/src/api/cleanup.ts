import { get, type Result, type Page } from './client'

export interface PendingImage {
  id: string
  imageId: string
  createdAt: string
}

export function getPendingImages(page = 1, size = 10): Promise<Result<Page<PendingImage>>> {
  return get(`/admin/cleanup/pending-images?page=${page}&size=${size}`)
}

export async function batchDeletePendingImages(ids: string[]): Promise<Result<null>> {
  return await (await fetch('/api/v1/admin/cleanup/pending-images', {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + localStorage.getItem('echocampus_token'),
    },
    body: JSON.stringify({ ids }),
  })).json()
}
