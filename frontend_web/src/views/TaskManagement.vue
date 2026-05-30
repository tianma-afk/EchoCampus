<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { vectorizeAllImages, getTaskList, getTaskStatus, type TaskVO } from '../api/task'

const tasks = ref<TaskVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const vectorizing = ref(false)

let pollTimer: ReturnType<typeof setInterval> | undefined
let refreshTimer: ReturnType<typeof setInterval> | undefined

const statusColor: Record<string, string> = {
  READY: '#9ca3af',
  PROCESSING: '#3b82f6',
  SUCCESS: '#10b981',
  FAILED: '#ef4444',
}

const statusLabel: Record<string, string> = {
  READY: '就绪',
  PROCESSING: '处理中',
  SUCCESS: '成功',
  FAILED: '失败',
}

const typeLabel: Record<string, string> = {
  VECTORIZE: '向量化',
  SEARCH: '图搜',
}

function shortId(id: string) {
  return id.substring(0, 8) + '...'
}

function formatTime(t: string | null) {
  if (!t) return '-'
  return new Date(t).toLocaleString()
}

async function fetchList() {
  try {
    const res = await getTaskList(page.value, pageSize.value)
    tasks.value = res.data.records
    total.value = res.data.total
  } catch {
    tasks.value = []
  }
}

function startPolling() {
  stopPolling()
  refreshTimer = setInterval(fetchList, 5000)
}

function stopPolling() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = undefined
  }
}

async function handleVectorize() {
  if (vectorizing.value) return
  vectorizing.value = true
  try {
    const res = await vectorizeAllImages()
    if (res.data) {
      await fetchList()
      pollSingleTask(res.data)
    } else {
      alert('暂无可向量化的图片')
    }
  } catch {
    alert('向量化任务提交失败，请重试')
  } finally {
    vectorizing.value = false
  }
}

function pollSingleTask(taskId: string) {
  const timer = setInterval(async () => {
    try {
      const res = await getTaskStatus(taskId)
      const task = res.data
      if (!task) {
        clearInterval(timer)
        return
      }
      if (task.taskStatus === 'SUCCESS') {
        clearInterval(timer)
        alert('向量化任务完成！')
        fetchList()
      } else if (task.taskStatus === 'FAILED') {
        clearInterval(timer)
        alert('向量化任务失败')
        fetchList()
      }
    } catch {
      // ignore polling errors
    }
  }, 3000)
}

onMounted(() => {
  fetchList()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <div class="task-management">
    <div class="page-header">
      <div class="page-title">
        <h1>任务管理</h1>
        <span class="page-count">共 {{ total }} 个任务</span>
      </div>
      <div class="page-actions">
        <button
          :disabled="vectorizing"
          class="vectorize-btn"
          @click="handleVectorize"
        >
          <el-icon :size="16"><VideoPlay /></el-icon>
          {{ vectorizing ? '提交中...' : '向量化所有图片' }}
        </button>
      </div>
    </div>

    <div class="table-card">
      <table class="data-table" v-if="!loading && tasks.length > 0">
        <thead>
          <tr>
            <th>任务ID</th>
            <th>类型</th>
            <th>算法任务ID</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="task in tasks" :key="task.id">
            <td class="mono" :title="task.id">{{ shortId(task.id) }}</td>
            <td>{{ typeLabel[task.taskType] ?? task.taskType }}</td>
            <td class="mono">{{ task.algTaskId ?? '-' }}</td>
            <td>
              <span
                class="status-tag"
                :style="{ backgroundColor: (statusColor[task.taskStatus] ?? '#9ca3af') + '20', color: statusColor[task.taskStatus] ?? '#9ca3af' }"
              >
                {{ statusLabel[task.taskStatus] ?? task.taskStatus }}
              </span>
            </td>
            <td class="time-cell">{{ formatTime(task.createdAt) }}</td>
            <td class="time-cell">{{ formatTime(task.updatedAt) }}</td>
          </tr>
        </tbody>
      </table>

      <div v-else-if="loading" class="empty-state">加载中...</div>
      <div v-else class="empty-state">暂无任务，点击"向量化所有图片"开始</div>

      <div class="pagination" v-if="total > pageSize">
        <button class="page-btn" :disabled="page <= 1" @click="page--; fetchList()">上一页</button>
        <span class="page-info">{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
        <button class="page-btn" :disabled="page >= Math.ceil(total / pageSize)" @click="page++; fetchList()">下一页</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.task-management {
  max-width: 1200px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-title h1 {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.page-count {
  font-size: 13px;
  color: #9ca3af;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.vectorize-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.vectorize-btn:hover:not(:disabled) {
  background: #2563eb;
}

.vectorize-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.vectorize-btn svg {
  width: 16px;
  height: 16px;
}

.table-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table thead {
  background: #f9fafb;
}

.data-table th {
  padding: 12px 16px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid #e5e7eb;
}

.data-table td {
  padding: 14px 16px;
  font-size: 14px;
  color: #374151;
  border-bottom: 1px solid #f3f4f6;
}

.data-table tbody tr:hover {
  background: #f9fafb;
}

.mono {
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

.status-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.time-cell {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
}

.empty-state {
  text-align: center;
  padding: 48px 0;
  color: #9ca3af;
  font-size: 14px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 16px;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: #10b981;
  color: #10b981;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: #6b7280;
}
</style>
