import { get } from './client'
import type { Result } from './client'

export interface CampusVO {
  id: string
  name: string
  universityId: string
}

export function searchCampuses(
  keyword: string,
  universityId?: string,
): Promise<Result<CampusVO[]>> {
  const params = new URLSearchParams()
  if (keyword) params.set('keyword', keyword)
  if (universityId) params.set('universityId', universityId)
  const qs = params.toString()
  return get<Result<CampusVO[]>>(`/admin/campuses/search${qs ? '?' + qs : ''}`)
}
