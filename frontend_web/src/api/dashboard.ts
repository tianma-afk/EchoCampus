import { get } from './client'
import type { Result } from './client'

export interface CategoryStat {
  categoryName: string
  count: number
}

export interface DashboardStats {
  landmarkCount: number
  universityCount: number
  pendingFeedbackCount: number
  taskCount: number
  categoryBreakdown: CategoryStat[]
}

export function getDashboardStats(): Promise<Result<DashboardStats>> {
  return get<Result<DashboardStats>>('/admin/dashboard/')
}
