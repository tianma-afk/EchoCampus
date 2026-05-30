import { get, type Result, type Page } from './client'

export interface TaskVO {
  id: string
  taskType: string
  algTaskId: string | null
  taskStatus: string
  createdAt: string
  updatedAt: string
}

export function vectorizeAllImages(): Promise<Result<string>> {
  return get('/admin/images/vectorize/all')
}

export function getTaskList(page: number, pageSize: number): Promise<Result<Page<TaskVO>>> {
  return get('/admin/images/tasks?page=' + page + '&pageSize=' + pageSize)
}

export function getTaskStatus(taskId: string): Promise<Result<TaskVO>> {
  return get('/admin/images/tasks/' + taskId)
}
