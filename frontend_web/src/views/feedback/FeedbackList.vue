<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getFeedbackList,
  FEEDBACK_TYPE_MAP,
  FEEDBACK_STATUS_MAP,
  type FeedbackAdminVO,
} from '../../api/feedback'

const router = useRouter()

const feedbacks = ref<FeedbackAdminVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const filterStatus = ref('')
const filterType = ref('')

const statusTagType: Record<string, string> = {
  PENDING: 'warning',
  RESOLVED: 'success',
  REJECTED: 'danger',
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getFeedbackList(
      page.value,
      pageSize.value,
      filterStatus.value || undefined,
      filterType.value || undefined,
    )
    feedbacks.value = res.data.records
    total.value = res.data.total
  } catch {
    feedbacks.value = []
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 1
  fetchList()
}

function goToDetail(id: string) {
  router.push(`/feedback/${id}`)
}

function truncateContent(text: string, maxLen: number): string {
  return text.length > maxLen ? text.slice(0, maxLen) + '…' : text
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="feedback-list">
    <div class="page-header">
      <div class="page-title">
        <h1>反馈审核</h1>
        <span class="page-count">共 {{ total }} 条反馈</span>
      </div>
    </div>

    <div class="filter-bar">
      <div class="filter-group">
        <label>状态</label>
        <el-select
          v-model="filterStatus"
          placeholder="全部"
          clearable
          style="width: 140px"
          @change="applyFilters"
        >
          <el-option label="待处理" value="PENDING" />
          <el-option label="已解决" value="RESOLVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </div>
      <div class="filter-group">
        <label>类型</label>
        <el-select
          v-model="filterType"
          placeholder="全部"
          clearable
          style="width: 140px"
          @change="applyFilters"
        >
          <el-option label="信息错误" value="INFO_ERROR" />
          <el-option label="违禁内容" value="CONTENT_ILLEGAL" />
          <el-option label="信息变更" value="INFO_CHANGE" />
          <el-option label="其他" value="OTHER" />
        </el-select>
      </div>
    </div>

    <div class="table-card">
      <el-table
        :data="feedbacks"
        v-loading="loading"
        highlight-current-row
        @row-click="(row) => goToDetail(row.id)"
      >
        <template #empty>
          <el-empty description="暂无反馈数据" />
        </template>

        <el-table-column label="地标名称" min-width="160">
          <template #default="{ row }">
            <span class="landmark-name">{{ row.landmarkName }}</span>
          </template>
        </el-table-column>

        <el-table-column label="反馈类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">
              {{ FEEDBACK_TYPE_MAP[row.feedbackType] || row.feedbackType }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="内容摘要" min-width="200">
          <template #default="{ row }">
            <span class="content-cell">{{ truncateContent(row.content, 30) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="statusTagType[row.status] ?? 'info'"
              size="small"
              effect="plain"
            >
              {{ FEEDBACK_STATUS_MAP[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="提交时间" width="130">
          <template #default="{ row }">
            <span class="time-cell">{{ new Date(row.createdAt).toLocaleDateString('zh-CN') }}</span>
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
.feedback-list {
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
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  letter-spacing: -0.02em;
}

.page-count {
  font-size: 13px;
  color: #9ca3af;
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-group label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.table-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.landmark-name {
  font-weight: 500;
  color: #1f2937;
}

.content-cell {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

.time-cell {
  white-space: nowrap;
  color: #6b7280;
  font-size: 13px;
}

.table-footer {
  display: flex;
  justify-content: center;
  padding: 16px;
  border-top: 1px solid #f3f4f6;
}

/* el-table style overrides */
:deep(.el-table th.el-table__cell) {
  background: #f8f9fb;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
}

:deep(.el-table .el-table__cell) {
  padding: 14px 16px;
}

:deep(.el-table__body tr:hover > td.el-table__cell) {
  background-color: #f8f9fb;
}

:deep(.el-table__body tr) {
  cursor: pointer;
  transition: background 0.15s;
}
</style>
