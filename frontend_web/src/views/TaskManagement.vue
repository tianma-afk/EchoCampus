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

const statusType: Record<string, string> = {
  READY: 'info',
  PROCESSING: '',
  SUCCESS: 'success',
  FAILED: 'danger',
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
  loading.value = true
  try {
    const res = await getTaskList(page.value, pageSize.value)
    tasks.value = res.data.records
    total.value = res.data.total
  } catch {
    tasks.value = []
  } finally {
    loading.value = false
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
        <el-button
          type="primary"
          :loading="vectorizing"
          @click="handleVectorize"
        >
          <el-icon :size="16"><VideoPlay /></el-icon>
          {{ vectorizing ? '提交中...' : '向量化所有图片' }}
        </el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table
        :data="tasks"
        v-loading="loading"
      >
        <template #empty>
          <el-empty description="暂无任务">
            <el-button type="primary" @click="handleVectorize">向量化所有图片</el-button>
          </el-empty>
        </template>

        <el-table-column label="任务ID" width="160">
          <template #default="{ row }">
            <span class="mono" :title="row.id">{{ shortId(row.id) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            {{ typeLabel[row.taskType] ?? row.taskType }}
          </template>
        </el-table-column>

        <el-table-column label="算法任务ID" width="140">
          <template #default="{ row }">
            <span class="mono">{{ row.algTaskId ?? '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="statusType[row.taskStatus] ?? 'info'"
              size="small"
              effect="plain"
            >
              {{ statusLabel[row.taskStatus] ?? row.taskStatus }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            <span class="time-cell">{{ formatTime(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">
            <span class="time-cell">{{ formatTime(row.updatedAt) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchList"
        />
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

.table-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.mono {
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

.time-cell {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
}

.table-footer {
  display: flex;
  justify-content: center;
  padding: 16px;
  border-top: 1px solid #f3f4f6;
}

/* el-table style overrides */
:deep(.el-table th.el-table__cell) {
  background: #f9fafb;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

:deep(.el-table .el-table__cell) {
  padding: 12px 16px;
}

:deep(.el-table__body tr:hover > td.el-table__cell) {
  background-color: #f9fafb;
}

:deep(.el-table__body tr) {
  transition: background 0.15s;
}

/* el-pagination style overrides */
:deep(.el-pagination .el-pager li.is-active) {
  background-color: #059669;
}

:deep(.el-pagination .el-pager li:hover) {
  color: #10b981;
}
</style>
