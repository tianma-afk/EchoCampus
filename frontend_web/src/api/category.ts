import { get } from './client'
import type { Result } from './client'

export interface CategoryVO {
  id: string
  name: string
}

export function listCategories(): Promise<Result<CategoryVO[]>> {
  return get<Result<CategoryVO[]>>('/admin/categories/')
}
