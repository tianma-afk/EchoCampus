import { get } from './client'
import type { Result } from './client'

export interface UniversityVO {
  id: string
  name: string
}

export function searchUniversities(keyword: string): Promise<Result<UniversityVO[]>> {
  const params = new URLSearchParams()
  if (keyword) params.set('keyword', keyword)
  const qs = params.toString()
  return get<Result<UniversityVO[]>>(`/admin/universities/search${qs ? '?' + qs : ''}`)
}
